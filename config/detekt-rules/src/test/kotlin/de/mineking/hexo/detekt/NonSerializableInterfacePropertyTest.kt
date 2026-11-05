package de.mineking.hexo.detekt

import dev.detekt.tooling.api.DetektCli
import org.intellij.lang.annotations.Language
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NonSerializableInterfacePropertyTest {
    @Test
    fun `rejects constructor body and inferred interface properties`() {
        val findings = lint(
            """
                interface Payload
                object DefaultPayload : Payload

                fun payload(): Payload = DefaultPayload
                
                @Serializable
                class Message(val direct: Payload, val nullable: Payload?) {
                    var body: Payload = DefaultPayload
                    val inferred = payload()
                }
            """,
        )

        assertEquals(4, findings.size, findings.toString())
        assertTrue(findings.all { "interface 'Payload'" in it })
    }

    @Test
    fun `checks nested generic arguments and aliases`() {
        val findings = lint(
            """
                interface Payload
                typealias PayloadAlias = Payload
                typealias Payloads = List<PayloadAlias?>

                @Serializable
                class Message(
                    val nested: Map<String, List<Payload?>>,
                    val aliased: Payloads,
                    val array: Array<Payload>,
                    val repeated: Map<Payload, Payload>,
                )
            """,
        )

        assertEquals(4, findings.size, findings.toString())
    }

    @Test
    fun `accepts serializable types and built-in collections`() {
        val findings = lint(
            """
                @Serializable sealed interface Payload
                @Serializable interface CustomPayload
                @Serializable data class Concrete(val value: Int)
                
                @Serializable
                class Message(
                    val direct: Payload,
                    val custom: CustomPayload?,
                    val concrete: Concrete,
                    val primitive: Int,
                    val text: String,
                    val bytes: ByteArray,
                    val items: List<Payload>,
                    val mutable: MutableList<String>,
                    val collection: Collection<String>,
                    val mutableCollection: MutableCollection<String>,
                    val set: Set<String>,
                    val mutableSet: MutableSet<String>,
                    val map: Map<String, Payload>,
                    val mutableMap: MutableMap<String, String>,
                )
            """,
        )

        assertEquals(emptyList(), findings)
    }

    @Test
    fun `accepts type-use serializers including aliases and nested arguments`() {
        val findings = lint(
            """
                interface Payload
                object DefaultPayload : Payload
                class Serializer
                typealias SerializablePayload = @Serializable(with = Serializer::class) Payload
                typealias RenamedPayload = SerializablePayload
                typealias Payloads = List<SerializablePayload?>

                @Serializable
                class Message(
                    val direct: @Serializable(with = Serializer::class) Payload,
                    val aliased: SerializablePayload,
                    val nullable: SerializablePayload?,
                    val renamed: RenamedPayload,
                    val items: Map<String, List<SerializablePayload?>>,
                    val aliases: Payloads,
                    val annotatedArgument: List<@Serializable(with = Serializer::class) Payload>,
                ) {
                    val body: SerializablePayload = DefaultPayload
                }
            """,
        )

        assertEquals(emptyList(), findings)
    }

    @Test
    fun `resolves type-use serializers from aliases in other source files`() {
        val findings = lint(
            """
                import model.SerializablePayload as WirePayload

                @Serializable
                class Message(val payload: WirePayload, val items: List<WirePayload?>)
            """,
            mapOf(
                "model/Payload.kt" to """
                    package model
                    import kotlinx.serialization.Serializable

                    interface Payload
                    class Serializer
                    typealias SerializablePayload = @Serializable(with = Serializer::class) Payload
                """.trimIndent(),
            ),
        )

        assertEquals(emptyList(), findings)
    }

    @Test
    fun `custom type serializer handles the entire generic type`() {
        val findings = lint(
            """
                interface Payload
                interface Container<T>
                class Serializer
                typealias SerializableContainer<T> = @Serializable(with = Serializer::class) Container<T>

                @Serializable
                class Message(
                    val aliased: SerializableContainer<Payload>,
                    val direct: @Serializable(with = Serializer::class) List<Payload>,
                    val mixed: Map<Payload, SerializableContainer<Payload>>,
                )
            """,
        )

        assertEquals(1, findings.size, findings.toString())
        assertTrue("Property 'mixed' uses interface 'Payload'" in findings.single())
    }

    @Test
    fun `type annotations without custom serializers do not exempt interfaces`() {
        val findings = lint(
            """
                interface Payload
                typealias AnnotatedPayload = @Serializable Payload
                typealias DefaultPayload = @Serializable(with = Nothing::class) Payload
                @Target(AnnotationTarget.TYPE)
                annotation class Unrelated

                @Serializable
                class Message(
                    val direct: @Serializable Payload,
                    val aliased: AnnotatedPayload,
                    val defaultSerializer: DefaultPayload,
                    val unrelated: @Unrelated Payload,
                )
            """,
        )

        assertEquals(4, findings.size, findings.toString())
    }

    @Test
    fun `skips non-serialized properties and ordinary classes`() {
        val findings = lint(
            """
                interface Payload
                object DefaultPayload : Payload
                class Ordinary(val payload: Payload)
                
                @Serializable 
                object Singleton { val payload: Payload = DefaultPayload }

                @Serializable
                class Message(@Transient val ignored: Payload = DefaultPayload) {
                    @Transient val body: Payload = DefaultPayload
                    val computed: Payload get() = DefaultPayload
                    val delegated: Payload by lazy { DefaultPayload }
                    fun local() { val payload: Payload = DefaultPayload }
                    class Nested(val payload: Payload)
                }
            """,
        )

        assertEquals(emptyList(), findings)
    }

    @Test
    fun `resolves qualified names, annotation aliases and other source files`() {
        val findings = lint(
            """
                import kotlinx.serialization.Serializable as WireSerializable
                import model.Payload as RenamedPayload

                @WireSerializable
                class Message(val aliased: RenamedPayload, val qualified: model.Payload)
            """,
            mapOf("model/Payload.kt" to "package model; interface Payload"),
        )

        assertEquals(2, findings.size)
        assertTrue(findings.all { "interface 'model.Payload'" in it })
    }

    @Test
    fun `does not accept contextual polymorphic or property serializer overrides`() {
        val findings = lint(
            """
                interface Payload
                class Serializer
                
                @Serializable
                class Message(
                    @Contextual val contextual: Payload,
                    @Polymorphic val polymorphic: Payload,
                    @Serializable(with = Serializer::class) val custom: Payload,
                )
            """,
        )

        assertEquals(3, findings.size)
    }

    @Test
    fun `ignores unrelated serializable annotations`() {
        val findings = lint(
            """
                annotation class Serializable
                interface Payload
                
                @Serializable class Message(val payload: Payload)
            """,
        )

        assertEquals(emptyList(), findings)
    }

    private fun lint(@Language("kotlin") code: String, otherSources: Map<String, String> = emptyMap()): List<String> {
        val directory = Files.createTempDirectory("serialization-rule-test")
        try {
            val sources = directory.resolve("src").createDirectories()

            /*language=kotlin*/
            sources.resolve("Annotations.kt").writeText("""
                package kotlinx.serialization
                import kotlin.reflect.KClass
                
                @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
                annotation class Serializable(val with: KClass<*> = Nothing::class)
                
                @Target(AnnotationTarget.PROPERTY)
                annotation class Transient
                
                @Target(AnnotationTarget.PROPERTY)
                annotation class Contextual
                
                @Target(AnnotationTarget.PROPERTY)
                annotation class Polymorphic
            """.trimIndent())

            sources.resolve("Message.kt").writeText("import kotlinx.serialization.*\n" + code.trimIndent())
            for ((name, source) in otherSources) {
                val path = sources.resolve(name)
                path.parent.createDirectories()
                path.writeText(source)
            }
            val config = directory.resolve("detekt.yml")
            config.writeText("custom:\n  active: true\n  NonSerializableInterfaceProperty:\n    active: true\n")

            val report = directory.resolve("report.xml")
            val output = StringBuilder()
            val result = DetektCli.load().run(
                arrayOf(
                    "--input", sources.toString(),
                    "--config", config.toString(),
                    "--analysis-mode", "full",
                    "--classpath", Unit::class.java.protectionDomain.codeSource.location.toURI().path,
                    "--disable-default-rulesets",
                    "--fail-on-severity", "Never",
                    "--report", "checkstyle:$report",
                ),
                output,
                output,
            )

            assertNull(result.error, output.toString())
            assertTrue(Files.exists(report), output.toString())

            val errors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report.toFile())
                .getElementsByTagName("error")

            return (0 until errors.length).map { errors.item(it).attributes.getNamedItem("message").nodeValue }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
