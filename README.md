# fast-markov

## Overview

"Fast Markov" is a [Markov model](https://hackernoon.com/from-what-is-a-markov-model-to-here-is-how-markov-models-work-1ac5f4629b71) generator with a learning capability. Its input is a file containing natural text to emulate, and its output consists of HTML documents displaying randomly generated "quotations." 

A learning mode is provided, in which quotes can be ranked as good, bad, or neutral in their quality by an human using a GUI. The following parameters of the model are iteratively adjusted based on this human feedback:

* The length of the randomly selected fragments used to piece together the output quotations
* The words eligible for use as the initial starting point for quote generation
* The content of the input file, which will grow to include not just real source text, but also "good" quotations 

A word about naming is in order. Only basic efforts have been made to make this code "fast" in terms of runtime performance. While obvious pitfalls in this area have hopefully been avoided, the code is mostly "fast" in the sense that it was put together quickly. Features or design decisions that would have increased its complexity without very clear benefit have been avoided.

## History

The system seen here grew out fan frustration with East Carolina University sports administrator Jon Gilbert and his tendency to send out long, platitude-filled emails. One message board participant said something like, "you could probably simulate Gilbert with a computer program," and thus "Fast Markov" was born. The provided text input file therefore consists of several of Gilbert's emails stitched together and cleaned up to remove sentence fragments and section headers.

## Using "Fast Markov"

"Fast Markov" is built using the [Porpus Leiningen template](https://github.com/beau1399/porpus). As such, it's a typical Figwheel / Jetty-based Web application. Running "lein figwheel" from a shell session in the root folder of the code will host a quote presentation page at http://localhost:3449, which you can refresh to see a new quote. The learning GUI (with a quote and three rating buttons) is hosted at http://localhost:3449/learn. 

## Generating Markov Models

You will likely want to use the code provided for something other than simulating Jon Gilbert. This can be done by replacing file "input" (present in the root folder of the code). You will also want to change file "byline," which is printed beneath the quote to give its putative author.

There are not many requirements for the content of the input file. It should end with a sentence terminator (. ? ! or a quote ending in one of these), or at least a word that occurs elsewhere in the text. You do not want to end the file with a word that does not exist anywhere else in the text, because this presents the Markov generator code with a token for which it cannot generate any reasonable followers.

In general, input text will yield better results if it hews closely to the rules of standard written English. Sentence fragments, unclosed quotations, section headers / outlines, etc. will serve to confuse the Markov logic. Sentence terminators should be followed by a space, which helps to distinguish them from other valid uses of these symbols. Abbreviations ending in periods may get confused for the ends of sentences, though configuration options for dealing with know abbreviations are discussed further below.

### The Constants File

The file at fast-markov/src/clj/fast_markov/constants.clj defines some constants that can be tweaked for optimal performance:

* Parameter *target-length* defines a minimum quote length. This is a soft limit, in that sentence fragments will be trimmed off of the end of the generated quote, so that quotes consist of full sentences.

* Parameters *min-phrase* and *max-phrase* define the range of the size of the fragments used to build quotes. The range is inclusive of its minimum but not its maximum. A value from the range will be randomly selected for each quote. The range is subject to the learning process; if a value of, say, 3 is selected for a quote, and that quote is rated good, then a 3 will be added to the in-memory range used for additional quotes. The range is also made persistent in file "lengths," which is loaded at startup, if available, in which case *min-phrase* and *max-phrase* are not used. 

* Values *dot-token*, *bang-token*, *quest-token*, and *comma-token* are placeholders used by the lexer. They should not require adjustment unless your input happens to include the default values supplied.

* Constant string *hidden-space* can be used within the input file to "glue together" words separated by space(s). For example, if you do not want "North" and "Dakota" to be treated as separate tokens, replace them with "North##Dakota" in the input (assuming ## is still the value present in constants.clj- you may need to change it for certain input text).

* Parameters *gui-rows* and *gui-cols* determine the size of the textarea control used by the "/learn" GUI.

* Constant *escaper-space* is a lexer placeholders similar to *dot-token*, etc.

### The Language File

The file at fast-markov/src/clj/fast_markov/language.clj contains some higher-level code that may be beneficial to tweak for your input text. In particular, the *units* data structure contains a list of regular expressions. Portions of the input that match one of these will be treated as an atom for purposes of quote generation. That is, these portions will not be split up into individual tokens, but will be treated as a single token.

The *units* expressions provided in the archive as downloaded will match quotations, expressions in parentheses, and several common abbreviations. You may find it necessary to add expressions for additional abbreviations, and you may also want to reconsider the treatement of quotes and parenthetical expressions. 

Including quotations in *units* was done under the assumption that these parts of the text do not represent the language of the person being emulated, but that of some other person he or she was quoting. If the input text is, say, taken from a work of literary fiction, in an attempt to emulate its author, that assumption is probably less valid than it is for the input text provided here.

The other thing present in "language.clj" is function *validate-quote*. This is applied near the end of the quote generation process, and ensures that the quote respects some basic linguistic rules. Quote marks and parentheses are balanced in its return value, for example, and this value will end in a complete sentence. 

### Learning Process Files

There are two important files that do not exist in the "Fast Markov" download but get generated automatically during the learning process. The file named "lengths," which contains a list of potential fragment lengths, has already been mentioned. File "starters" is similar; it is a persistent storage mechanism for the list of valid quote-starting words.

In both cases, these files get changed during the learning process, and are paralleled by an in-memory runtime data structure. Both files consist of a collection of newline-delimited values; in "starters," these are individual words, and it "lengths" they are integer fragment lengths. Both files can be deliberately hand-crafted prior to the learning process, or omitted and allowed to be automatically built when the learning process begins.

File "lengths" will default, as already described, to a list of the integers greater than or equal to *min-phrase* and less than *max-phrase*. File "starters" will be built, if it has not been previously created, such that it includes all sentence-starters in the input text. 

## Technical Description

The quote generation process relies on several steps; a reasonable breakdown of these might talk about lexing the input text into tokens, parsing this result into data structures meaningful to the Markov chain generator, and then applying the generator to get a quote. 

Each of these steps occurs repeatedly. It would be faster at runtime to do the lexing and parsing just once, but a couple of aspects of the system's function prevent this. First, the randomized nature of the size of the quote fragments used as building blocks implies that the data structure that drives the Markov process will take different forms over time. Second, the inclusion of a learning process implies similar changes.

One implication of this repeated lex / parse execution is that the data structure used to map words to followers and their frequency does not need to be particularly optimal. One might be tempted to implement this as a map, for example, to obtain a quick lookup time, e.g.:

```clojure
{"I" '("want to" "need to" "must have") "You" '("should have" "are not" "must not")}
```

Certainly, if this data were more enduring during program execution, this would be advisable. However, given the ephemeral nature of the input text, the fragment size, and the quote starter tokens, any improvement in lookup time would be offset by the time spent repeatedly building of such a map. Instead, the data seen above is maintained in a structure like this:

```clojure
'( '("I" "want" "to") '("I" "need" "to") '("I" "must" "have")
   '("You" "should" "have") '("You" "are" "not")  '("You" "must" "not") )
```
This data structure is used as input (as parameter *maps*) to a function called *words-for*, which returns potential followers for a word passed as parameter:

```clojure
(defn words-for
  [word maps] (map rest (filter #(= (first %) word ) maps)))
```

### Lexing 

The lexing process translates the input text into a series of tokens. Most of these will simply be words, stored unmodified in individual strings. Syntactically important things like sentence terminators and text matching one of the unit patterns from "language.clj" will be transformed into semantic tokens defined in "constants.clj."

#### Initial Cleaning

On program start, "input" is read and put through some preprocessing steps that make it more consistent and easier to deal with:

* Whitespace is collapsed, replacing newlines and consecutive whitespace with single spaces. 

* Unicode quotes are replaced with their ASCII equivalents

* A space is added to the end of the text, to ensure that any final sentence terminator is recognizable as such.

#### Unitization

Next, "units" in the text are identified, based on the data present in "language.clj." These then have any contained whitespace replaced with a temporary placeholder. The abbreviation units are defined such that the trailing space gets included and escaped. The rest of the code is largely based on splitting up tokens wherever a space occurs, and this will skip over any text unitized in this manner.

The unitization code may be of interest to some readers. Each regular expression is first passed into *find-units*, which returns a function that finds all of its occurrences (except those already unitized under some other regular expression):

```clojure
(defn find-units [regex] (fn[p]
                           (filter #(not (re-matches #"[^\s]+" %))
                                   (re-seq regex p))))
```
This is applied to the input text, and then passed to *esc-functions*, which generates a list of replacement functions for the actual snippets of text found:

```clojure
(defn esc-functions[snippets]
  (map
   (fn[p] #(str/replace % p
                        (str " "
                             (str/.replace p  " " const/escaper-space) 
                        " "))) snippets))
```

These are composed and applied to the input text by *unitize*:

```clojure
(defn unitize[find-func txt]
  ((apply comp (esc-functions (find-func txt))) txt))
```

The *unitize* function is itself composed by *unitize-all*, which orchestrates the whole process:

```clojure
(defn unitize-all [txt]
  ((apply comp (map (fn[p]  #(unitize (find-units p) %)) lang/units)) txt))
```
This could all be accomplished less verbosely, but breaking up the process into several functions did allow for more intelligibility during development.

### Lexing ###

blah, blah

#### Unitization


