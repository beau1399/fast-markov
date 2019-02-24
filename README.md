# fast-markov

## Overview

"Fast Markov" is a [Markov model](https://hackernoon.com/from-what-is-a-markov-model-to-here-is-how-markov-models-work-1ac5f4629b71) generator with a learning capability. Its input is a file full of natural text to emulate, and its output consists of HTML documents displaying randomly generated "quotations." 

A learning mode is provided, in which quotes can be ranked as good, bad, or neutral in their quality by an human using a GUI. The following parameters of the model are iteratively adjusted based on this human feedback:

* The length of the randomly selected fragments used to piece together the output quotations
* The words eligible for use as the initial starting point for quote generation
* The content of the input file, which will grow to include not just real source text, but also "good" quotations 

A word about naming is in order. Only basic efforts have been made to make this code "fast" in terms of runtime performance. While obvious pitfalls in this area have hopefully been avoided, the code is mostly "fast" in the sense that it was put together quickly. Features or design decisions that would have increased its complexity without very clear benefit have been avoided.

## History

The system seen here grew out fan frustration with East Carolina University sports administrator Jon Gilbert and his tendency to send out long, platitude-filled emails to fans and alumni. One message board participant said something like, "you could probably simulate Gilbert with a computer program, and thus "Fast Markov" was born. The provided text input file thus consists of several of Gilbert's emails stitched together and cleaned up to remove sentence fragments and section headers.

## Using "Fast Markov"

"Fast Markov" is built using the [Porpus Leiningen template](https://github.com/beau1399/porpus). As such, it's a typical Figwheel / Jetty-based Web application. Running "lein figwheel" from a shell session in the root folder of the code will host a quote presentation page at http://localhost:3449, which you can refresh to see a onew quote, and the learning GUI (with a quote and three rating buttons) at http://localhost:3449/learn. 

## Generating Markov Models

You will likely want to use the code provided for something other than simulating Jon Gilbert. This can be done by replacing file "input" (present in the root folder of the code).

There are not many requirements for the content of the input file. It should end with a sentence terminator (. ? ! or a quote ending in one of these), or at least a word that occurs elsewhere in the text. You do not want to end the file with a word that does not exist anywhere else in the text, because this presents the Markov generator code with a token for which it cannot generate any reasonable followers.

In general, input text will yield better results if it hews closely to the rules of standard written English. Sentence fragments, unclosed quotations, section headers / outlines, etc. will serve to confuse the Markov logic. Abbreviations ending in periods may get confused for the ends of sentences, though configuration options for dealing with know abbreviations are discussed further below.

### The Constants File

The file at fast-markov/src/clj/fast_markov/constants.clj defines some constants that can be tweaked for optimal performance:

* Parameter *target-length* defines a minimum quote length. This is a soft limit, in that sentence fragments will be trimmed off of the end of the generated quote, so that quotes consist of full sentences.

* Parameters *min-phrase* and *max-phrase* define the range of the size of the fragments used to build quotes. The range is inclusive of its minimum but not its maximum. A value from the range will be randomly selected for each quote. The range is subject to the learning process; if a value of, say, 3 is selected for a quote, and that quote is rated good, then a 3 will be added to the in-memory range used for additional quotes. The range is also made persistent in file "lengths," which is loaded at startup, if available, in which case *min-phrase* and *max-phrase* are not used. 

* Values *dot-token*, *bang-token*, *quest-token*, and *comma-token* are placeholders used by the lexer. They should not require adjustment unless your input happens to include the default values supplied.

* Constant string *hidden-space* can be used within the input file to "glue together" words separated by space(s). For example, if you do not want "North" and "Dakota" to be treated as separate tokens, replace them with "North##Dakota" in the input (assuming ## is still the value present in constants.clj- you may need to change it for certain input text).

* Parameters *gui-rows* and *gui-cols* determine the size of the textarea control used by the "/learn" GUI.

* Constants *escaper-quote* and *escaper-dot* are lexer placeholders similar to *dot-token*, etc.

### The Language File

The file at fast-markov/src/clj/fast_markov/language.clj contains some higher-level code that may be beneficial to tweak for your input text:

* The *units* data structure contains a list of regular expressions. Portions of the input that match one of these will be treated as an atom for purposes of quote generation. That is, these portions will be 

### Learning Process Files

There are two important files that do not exist in the "Fast Markov" download but get generated automatically during the learning process. The file named "lengths," which contains a list of potential fragment lengths, has already been mentioned. File "starters" is similar; it is a persistent storage mechanism for the list of valid quote-starting words.

In both cases, these files get changed during the learning process, and are paralleled by an in-memory runtime data structure. Both files consist of a collection of newline-delimited values; in "starters," these are individual words, and it "lengths" they are integer fragment lengths. Both files can be deliberately hand-crafted prior to the learning process, or omitted and allowed to be automatically built when the learning process begins.

File "lengths" will default, as already described, to a list of the integers between *min-phrase* and *max-phrase*. File "starters" will be built, if it has not been previously created, such that it includes all sentence-starters in the input text. 

## Technical Description

blah, blah

### Lexing ###

blah, blah

#### Unitization


