# TODO

### General

- [ ] OpenBench
- [ ] Cloud-hosted OB solution (with pythonanywhere?)
- [ ] FRC/DFRC support
- [ ] Improve javadoc
- [ ] Review all code comments
- [x] Reduce artifact size (remove plugins, dependencies, delete old nets)
- [ ] Refactor/improve TranspositionTable code
- [ ] Move all stacks (board state, accumulator, history etc.) to single thread stack
- [ ] Check LazySMP still works
- [ ] Put Calvin back on Lichess
- [ ] Stop using int everywhere (use byte/short where possible)
- [ ] Find solution to CCRL/ChessGUI/Vector API issue

### Search

- [ ] SPRT SPSA tune branch
- [ ] LTC 1024 net
- [ ] Threefold repetition ( CCRL bug ? )
- [ ] TT cut after RFP if depth - quietReduction <= ttDepth?
- [ ] Fractional depth
- [ ] Improving rate
- [ ] PV table
- [ ] Exponentially widening aspiration window
- [ ] Write to TT during QS stand-pat cutoff
- [ ] Singular extensions ( tried )
- [ ] Double extensions
- [ ] Triple extensions
- [ ] Negative extensions
- [ ] MultiCut ( tried )
- [ ] ProbCut ( tried )
- [ ] Use improving in NMP
- [x] Pawn corrhist
- [x] Non-pawn corrhist
- [ ] Material corrhist
- [ ] Major corrhist ( tried )
- [ ] Minor corrhist ( tried )
- [ ] Contcorrhist
- [ ] Threat corrhist ( tried, failed SPRT )
- [ ] Countermove corrhist?
- [ ] Follow-up move corrhist?
- [ ] TT-move corrhist? ( tried, failed SPRT )
- [x] TT score eval correction
- [ ] LMP table
- [ ] Reduce killers less
- [x] Razoring
- [x] History pruning
- [x] Reverse futility reductions
- [x] Futility reductions
- [ ] SEE pruning ( tried )
- [ ] Faster SEE ( tried )
- [ ] Syzygy TB
- [ ] Cuckoo hashing repetition detection
- [x] Check root legal moves, if only one, play instantly
- [ ] Merge Searcher and ParallelSearcher

### Move generation / Move ordering

- [ ] Pseudo-legal movegen ( tried, didn't work, possible skill issue )
- [ ] Implement isLegal(move) re-using pin/checker info from movegen
- [x] The Great `MovePicker` Rewrite`™`
- [ ] Try copy/make rather than make/unmake
- [x] 1-ply Conthist
- [x] 2-ply Conthist
- [ ] 4-ply Conthist
- [x] Capthist
- [ ] Don't update killers during null-move search?
- [ ] Countermoves ( tried )
- [ ] Threat quiet history ( tried )
- [ ] Threat capture history 
- [ ] Increase history bonus/penalty when alpha > beta + 50
- [ ] Score-based history bonuses
- [ ] PSQT-based move ordering bonus
- [ ] Add Killers, History etc. stages to `MovePicker`. ( tried )
- [ ] Other magic bitboard impls? PEXT/Black Magic/Kindergarten/whatever?
- [ ] Add checkers bitboard to `Board`?
- [ ] Check if move is check before makeMove

### Evaluation

- [ ] Lazy NNUE updates ( tried )
- [x] New net, bigger HL size
- [x] Screlu
- [ ] Output buckets ( tried, material scaling seems stronger )
- [ ] Horizontal mirroring
- [ ] Datagen
- [ ] Input (king) buckets
- [ ] Finny tables

### Transposition Table

- [x] Always store eval in TT (should be 5-ish elo?)
- [ ] PV node flag in TT
- [ ] 16-bits zobrist in TT? (SF does it)
- [ ] Fully compress all fields to minimum size
- [ ] Remove bucketing?
- [ ] Other replacement schemes?

### Time management

- [x] Node TM
- [ ] Legal move based TM?
- [ ] Futility TM?
- [ ] Game phase TM?
- [ ] Single reply/forced move TM?
- [ ] Best move changes TM? 

### UCI

- [x] Add engine author/version
- [x] Add perft to UCI options
- [x] Implement display command
- [ ] Improve display visuals + add info
- [ ] Implement perft divide
- [ ] Standardise 'help' info
- [ ] Add seldepth to info
- [x] Add hashfull to info
- [ ] MultiPV
- [x] Coloured output

