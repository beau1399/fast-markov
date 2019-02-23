# fast-markov

"Fast Markov" is a [Markov model](https://hackernoon.com/from-what-is-a-markov-model-to-here-is-how-markov-models-work-1ac5f4629b71) generator with a learning capability. Its input is a file full of natural text to emulate, and its output consists of HTML documents displaying randomly generated "quotations." 

A learning mode is provided, in which quotes can be ranked as good, bad, or neutral in their quality by an human using a GUI. The following parameters of the model are iteratively adjusted based on this human feedback:

* The length of the randomly selected fragments used to piece together the output quotations
* The words eligible for use as the initial starting point for quote generation
* The content of the input file, which will grow to include not just real source text, but also "good" quotations 
