# Binary board format, version 1

## Primitives

Bits are least-significant-first within fields and bytes, without alignment.
Only the final byte is zero-padded; extra bytes or nonzero padding are invalid.

- `B(k)`: unsigned integer in `k` bits; `B(0)` stores nothing.
- `U`: shortest unsigned LEB128, at most 5 bytes, range `0..2^33-1`.
- `P(x) = U(x-1)` for positive `x`.
- `S(x) = U(2*x)` if `x >= 0`, otherwise `U(-2*x-1)`.
- `w(x) = ceil(log2(x))`.

Coordinates and turns fit signed 32-bit integers. Compute differences and
reconstructed values without overflow.

## Structure

Header fields, in order: `B(3) version=1`, `B(1) cellsPresent`,
`B(1) linesPresent`, `B(2) turnDisplay` (absent/null=0, false=1, true=2; 3 invalid).
Absent and null are identical; decoding value 0 leaves the display setting unset.
The indicated cell and line sections follow, in that order. Empty board: `01` hex.

**Cells:** `P(N)`, coordinates, `Symbols(N,3)` ownership, `B(3)` metadata mask,
then indicated streams in mask-bit order. Ownership: absent=0, X=1, O=2.
Coordinates are unique and sorted by `(r,q)`; all cell streams use this order.
Omit ownerless cells without highlights and with empty/whitespace-only labels,
regardless of focus or turn.

## Coordinates

`B(1)` selects sparse=0 or dense=1:

- **Sparse:** `B(1)` selects deltas=0 or rows=1.
  - Deltas: `N` pairs `S(q-previousQ), S(r-previousR)`, initially `(0,0)`.
  - Rows: first `S(r)`, later `P(r-previousR)`; then `P(rowCellCount)`,
    `S(firstQ)`, and `P(q-previousQ)` for remaining columns. Repeat to total `N`.
- **Dense:** `S(minQ), S(minR), P(width), P(height), B(1) full`.
  Full=1 includes all positions; otherwise `width*height` presence bits follow,
  increasing `q` first, then `r`. Population must equal `N`. Dimensions are
  at most `2^32`, area at most `2^63-1`; coordinate bounds fit signed 32-bit.
  Encoding uses the smallest enclosing rectangle.

## Metadata

| Mask bit | Stream                                        | Omitted default |
|----------|-----------------------------------------------|-----------------|
| 0        | `Symbols(N,4)`: absent=0, neutral=1, X=2, O=3 | No highlight    |
| 1        | Turns                                         | No turn         |
| 2        | Labels                                        | Empty label     |

Turns and labels start with `Symbols(N,2)` presence (absent=0, present=1).
Let `M > 0` be the number of present entries; values follow in coordinate order.

- **Turns:** `B(1)` selects `M` absolute `S(turn)` values (0) or
  `S(turn-previousTurn)` values (1), initially zero.
- **Labels:** `B(1)` selects `M` strings (0) or a dictionary (1): `P(K)`,
  `K` distinct decoded strings in first-occurrence order, then `M` indices
  `B(w(K))`, with `1 <= K <= M` and indices `< K`.
- **String:** `P(byteLength)`, then UTF-8 bytes. Malformed UTF-8 is decoded
  with U+FFFD replacement characters.

## Symbol streams

`Symbols(n,R)` has implicit count `n`, alphabet `0..R-1`, and a `B(2)` mode:

- **0, uniform:** one `B(w(R))` symbol, repeated `n` times.
- **1, packed:** packed symbols as below.
- **2, sparse:** default `d` in `B(w(R))`, `P(K)` exceptions (`1 <= K <= n`),
  then `K` gaps `P(index-previousIndex)`, initially `previousIndex=-1`;
  indices must be `< n`. Exceptions are packed with radix `R-1`, removing `d`
  from the alphabet and renumbering the remaining values from zero.
- **3:** invalid.

Packing: blocks of 8, 5, or 4 symbols for radix 2, 3, or 4. A block of `m`
symbols stores `sum(symbol[i]*R^i, i=0..m-1)` in `B(w(R^m))`; only the last block may
be shorter. Codes `>= R^m` are invalid. Radix 1 uses no bits. All symbols
must be within their alphabet.

## Line highlights

`P(L)`, then `L` records: `S(q-previousQ), S(r-previousR), B(8) descriptor`.
Previous start is initially `(0,0)`; record order and duplicates are preserved.

`descriptor = (length-1)*18 + direction*3 + color`

Length: 1–12 cells including start. Color: neutral=0, X=1, O=2.
Directions 0–5: `(1,0), (0,1), (-1,1), (-1,0), (0,-1), (1,-1)`.
Descriptors 216–255 are invalid.

## Encoding rules

Omit all-default metadata streams; use uniform symbols whenever possible.
Otherwise, choose the shortest representation in bits. Tie priorities:
coordinates deltas, rows, dense; turns absolute, deltas; labels direct,
dictionary; symbols packed, sparse with the lowest default. Unsupported versions,
truncation, invalid counts, and duplicate dictionary entries are invalid.
