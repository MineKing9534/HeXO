@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.board.render

import de.mineking.hexo.board.Board
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

interface BoardRenderer<P, R : Any> {
    suspend fun render(board: Board, param: P): R
}

suspend fun <R : Any> BoardRenderer<Unit, R>.render(board: Board) = render(board, Unit)

fun <P, R : Any> BoardRenderer<P, R>.fixedParam(param: P) = object : BoardRenderer<Unit, R> {
    val param = param
    override suspend fun render(board: Board, param: Unit) = this@fixedParam.render(board, this.param)
}

fun <P, R : Any> BoardRenderer<P, R>.limitConcurrency(maxConcurrency: Int) = object : BoardRenderer<P, R> {
    private val semaphore = Semaphore(maxConcurrency)

    override suspend fun render(board: Board, param: P) = semaphore.withPermit {
        this@limitConcurrency.render(board, param)
    }
}

fun <P> BoardRenderer<P, String>.outputUtf8Bytes() = object : BoardRenderer<P, ByteArray> {
    override suspend fun render(board: Board, param: P) = this@outputUtf8Bytes.render(board, param).encodeToByteArray()
}
