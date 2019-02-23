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

The file at fast-markov/src/clj/fast_markov/constants.clj

### The Language File

## Technical Description

blah, blah

### Lexing ###

blah, blah

#### Unitization


