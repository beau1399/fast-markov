(ns fast-markov.constants)

;;;Affects but does not dictate the length of generated quotes; quotes will begin
;;; at least this long, but will also be pared down to remove sentence fragments,
;;; for example.
(def target-length 200)

;;; Lower and (non-inclusive) upper bound on the size of the fragments of known
;;;  consecutive words used to build output
(def min-phrase 2)
(def max-phrase 9)

;;; These are used to separate and mark sentence enders so that their syntax
;;;  value can be considered properly.
(def dot-token "_DOT_")
(def bang-token "_BANG_")
(def quest-token "_QUEST_")
(def comma-token "_COMMA_")

;;; Manually put this into the input to denote words that are space-delimeted
;;;  but should be treated as a unit. (Quick alternative to units in
;;;  language.clj)
(def hidden-space "##")

;;; For textarea that displays quote in "learning" mode
(def gui-rows 12)
(def gui-cols 80)

;;;In strings that get turned into units according to the logic in language.clj,
;;; this is accomplished by removing temporarily anything that has significance
;;; to the fast-markov lexer, i.e. spaces and dots.
(def escaper-space "~~@") ;Temp. whitespace marker in units defined in language.clj
(def escaper-dot "~~*")   ;Similar, but for dots (lest they get treated as periods)
