**k-hop neighbors discovery in a distributed system**
----------------------------------------------------

THis program creates a distributed system consisting of n nodes arranged in a certain topology specified in a configuration file and implements a distributed algorithm that enables each node to discover the entire network and identify its k-hop neighbors for each k. The algorithm eventually terminates at each node after which a node will output k-hop neighbors for each k.

**Configuration File Format**

The configuration file must be a plain-text formatted file no more than 100KB in size. Only lines which begin with an unsigned integer are considered to be valid. Lines which are not valid are ignored. The configuration file must contain 2n + 1 valid lines. The first valid line of the configuration file must contain one token denoting the number of nodes in the system. After the first valid line, the next n lines must consist of three tokens each. The first token is the node ID. The second token is the host-name of the machine on which the node runs. The third token is the port on which the node listens for incoming connections.
Finally, the next n lines must consist of upto n tokens each. The first token is the node ID. The next up to n - 1 tokens must denote the IDs of the direct neighbors (or 1-hop neighbors) of the node referred to by the first token. The parser is written so as to be robust concerning leading and trailing white space or extra lines at the beginning or end of file, as well as interleaved with valid lines. The # character will denote a comment. On any valid line, any characters after a # character are ignored.

**Example confiuration file**

5

0 host0 1234

1 host1 1233

2 host2 1233

3 host3 1232

4 host4 1233

0 1 2 4

1 0 2 3

2 0 1 3

3 1 2

4 0
