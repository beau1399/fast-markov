(ns fast-markov.constants)


;;;Affects but does not dictate the length of generated quotes; quotes will begin
;;; at least this long, but will also be pared down to remove sentence fragments,
;;; for example.
(def target-length 200)
