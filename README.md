# Find Sentences Similarity Among Log messages with Specific Format  
This implementation go over log file and log into output similar sentences.
According the task description I assume all sentences are in the same format and consist from 5 parts:  
[date] [time] [name] is [action] [rest of sentence]   
For example in the log message:
"01-02-2012 20:12:39 Naomi is sitting in the restaurant"  
name = "Naomi"  
action = "sitting"  
rest = "in the restaurant"   
Each part of the sentence is a type. The types are NAME, ACTION, REST   

**Format of results**  
Upon finding sentences with only 1 word change, the output should be:  
sentence 1  
sentence 2  
sentence 3  
The changing word was: [a] ,[b], [c]   

For example, for input:  
01-01-2012 20:12:33 Lea is doing something  
01-01-2012 20:12:32 Lea is making something  
The ouput is:  
01-01-2012 20:12:33 Lea is doing something  
01-01-2012 20:12:32 Lea is making something 
The changing word was:  doing, making

**Algorithm**  
The naive implementation would compare all sentences to all sentences.  
A better implementation would index the data during reading the sentences and compare each new sentence to the indexed data.
The algorithm go over sentences. Since need to log out the full sentence including the hour as output, implementation must save all sentences read from input file into a list.  
The indexing process, save during running maps of the 3 types (name, action, rest). Each map contains:  
key -  (name/action/rest)  
value -  set of integers which represents the sentence index where this word exists.  
For example upon reading the following sentence:  
Naomi is sitting at the car  
It will save:  
"Naomi" into mapping of names to a set of sentences index were this name exists.  
The same is done with the action and rest of the sentences.  
  
During looping on the sentences, for each 2 parts of sentences, where intersections of the sentence index give more than 1 result it is a match!!  
The results are saved into a map in which the key is a string concatenating the 2 identical actions and the value is a set of sentence indexes where this match exists.  

**Alternative algorithm**  
Alternative algorithm would be to save each combination of 2 parts found into a mapping:  
 key - combination of 2 parts (name_action/name_rest/action_rest)  
 value - indexes where these combination exists  
 At the end of run, go over all entries in this map and output set where at least 2 indexes exists.    
 

**Complexity**  
The time complexity is O(n). Just going over all the sentences.
space complexity is more triki.
We holds all sentences which is O(n) and for each word a set of indexes.  

**Sentences not according the format**  
A sentense not according format will not fail the process.
In this case an error is written into the logger.
At the end of run there is error written into logger and console containning all these sentences.  

**How to implement when no time limit**  

* The most expensive action is reading from a file. Read data in chunks.
* In case of huge file, see relevant link for dealing with huge files data: https://itnext.io/using-java-to-read-really-really-large-files-a6f8a3f44649 
* The processing can be done in parallel as there is no importance to the order of rows.

 * Use database in order to scale and avoid high memory usage problems.
We can have cache for high occurense templates.  

 * Write the code in a more generic way so it can be used for any template. Currently is serves template containing 3 parts.
 * Use pattern recognition algorithm in order to support any pattern.