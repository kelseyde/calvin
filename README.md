<img src="src/main/resources/calvin.png" width="120">

A chess engine written in Java. Named after my favourite comic book character.

This is a personal project. I am a professional Java developer and amateur chess player, and so I decided to combine these two things and dive into the world of chess programming. I have never had so much fun writing code in my life.

Calvin is still a pretty average chess player (~1500 on Lichess as of October 2023), but his rating slowly climbs as I add things here and there. 

Current feautures include:

- Bitboard board representation
- Pseudo-legal move generation
- Magic bitboards for sliding piece move generation
- An iterative deepening search framework using negamax, with an added quiescence search at the end to handle 'noisy' positions
- Move ordering: previous best move, MVV-LVA, killer heuristic, history heuristic.
- Transposition tables
- Zobrist hashing
- Evaluation: basic material count and piece square tables, pawn structure, passed pawn bonuses, isolated/doubled pawn penalties
- UCI protocol implemented and Calvin is connected to Lichess where he plays regularly in the bot pool: https://lichess.org/@/Calvin_Bot

If you would like to contribute, or just talk about chess/chess programming, get in touch!

<img src="src/main/resources/hobbes.png" width="140">
