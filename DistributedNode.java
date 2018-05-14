package com.arun.advancedOS;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Arun
 * 
 *         Implementation of Round Robin Algorithm for discovery of nodes in a
 *         distributed system, where each node only has knowledge about its
 *         1-hop neighbours. The output depicts the hop distance to each every
 *         other node.
 *
 */
public class DistributedNode {
	public static void main(String[] args) throws IOException {

		int portNumber = 3001, neighbourCount = 0, nodeId = 0, termination = 0, globalTermination = 0, round = 0, roundHopId;
		int noneCount = 0;
		String hostname = InetAddress.getLocalHost().getHostName(), parts[], line, message = "";
		boolean lastRound = false, localTerminated = false,noneAlready=false;
		Queue<String> buffer = new ConcurrentLinkedQueue<>();
		TreeMap<Integer, Host> hosts = new TreeMap<>();
		TreeMap<Integer, Integer> neighbourHops = new TreeMap<>();
		TreeMap<Integer, Integer> kHops = new TreeMap<>();
		TreeMap<Integer, String> groupHop = new TreeMap<>();
		Socket socket = new Socket();
		SocketAddress socketAddress;
		PrintWriter send;
		BufferedReader receive;

		Scanner s = new Scanner(new File(
					"config.txt"));
		line = s.nextLine();
		
		// To skip invalid lines and comments
		while (line.isEmpty() || line.startsWith("#"))
			line = s.nextLine();
		
		// Get the total Number of nodes in the system
		int N = Integer.parseInt(line.trim());
		for (int i = 0; i < N; i++) {
			line = s.nextLine().trim();
			while (line.isEmpty() || line.startsWith("#"))
				line = s.nextLine().trim();
			parts = line.split("\\s+");
			if (parts[1].equals(hostname)) {
				nodeId = Integer.parseInt(parts[0]);
				portNumber = Integer.parseInt(parts[2]);
			}
			hosts.put(Integer.parseInt(parts[0]),
					new Host(parts[1], Integer.parseInt(parts[2])));
		}

		for (int i = 0; i < N; i++) {
			line = s.nextLine();
			while (line.isEmpty() || line.startsWith("#"))
				line = s.nextLine();
			parts = line.trim().split("\\s+");
			if (Integer.parseInt(parts[0]) == nodeId) {
				neighbourCount = parts.length - 1;
				for (int k = 1; k < parts.length; k++)
					neighbourHops.put(Integer.parseInt(parts[k]), 1);
				break;
			}
		}
		kHops.putAll(neighbourHops);

		try (ServerSocket serverSocket = new ServerSocket(portNumber);) {
			//Run rounds until global termination happens
			while (true) {
				round++;
				if (globalTermination >= neighbourCount) {
					if (localTerminated == true)
						break;
					message = String.valueOf(round) + " " + "done";
					lastRound = true;
				} else if (termination + globalTermination >= neighbourCount) {
					message = String.valueOf(round) + " " + "done";
					localTerminated = true;
				} else {
					message = String.valueOf(round) + " ";
					for (Entry<Integer, Integer> entry : kHops.entrySet()) {
						if (entry.getValue() == round)
							message += entry.getKey().toString() + " ";
					}
					if(noneAlready==false && noneCount<neighbourCount)
					if (message.split(" ").length == 1){
						message += "none";
						noneAlready=true;
					}
				}

				//Connect to the neighbour scocket and send the message
				for (Integer hopNodeId : neighbourHops.keySet()) {
					Host neighbour = hosts.get(hopNodeId);
					socketAddress = new InetSocketAddress(neighbour.hostname,
							neighbour.port);
					boolean scanning = true;
					while (scanning) {
						try {
							socket = new Socket();
							socket.connect(socketAddress);
							scanning = false;
						} catch (ConnectException e) {
							System.out
									.println("Connect failed, waiting and trying again");
							try {
								Thread.sleep(2000);// 2 seconds
							} catch (InterruptedException ie) {
								ie.printStackTrace();
							}
						}
					}
					send = new PrintWriter(socket.getOutputStream(), true);
					System.out.println("Sending message" + message + " Port: "
							+ neighbour.port);
					send.println(message);
					socket.close();
				}

				//Resetting the termination variables
				termination = 0;
				globalTermination = 0;
				noneCount=0;
				int in = 0;
				
				//Process all the messages present inside the buffer, received from the previous round.
				while (!buffer.isEmpty()) {
					message = buffer.poll();
					System.out
							.println("Received message" + message + " Port: ");
					parts = message.split(" ");
					if (parts.length == 1)
						termination++;
					else if (message.endsWith("done"))
						globalTermination++;
					else if (message.endsWith("none")) {
						noneCount+=1;
					} else {
						for (int i = 1; i < parts.length; i++) {
							roundHopId = Integer.parseInt(parts[i]);
							if (!kHops.containsKey(roundHopId)
									&& nodeId != roundHopId) {
								kHops.put(roundHopId, round + 1);
							}
						}
					}
					in++;
				}
				//Receive the messages from all the 1 hop neighbours
				while (in < neighbourCount) {
					socket = serverSocket.accept();
					receive = new BufferedReader(new InputStreamReader(
							socket.getInputStream()));
					message = receive.readLine();
					
					//Buffer the messages of next round
					if (!message.startsWith(String.valueOf(round))) {
						buffer.add(message);
						continue;
					}
					System.out
							.println("Received message" + message + " Port: ");
					
					//Local termination message from neighbour
					parts = message.split(" ");
					if (parts.length == 1)
						termination++;
					
					//Global termination message from neighbour
					else if (message.endsWith("done"))
						globalTermination++;
					
					else if (message.endsWith("none")) {
						noneCount +=1;
					} 
					
					//K hop neighbour information
					else {
						for (int i = 1; i < parts.length; i++) {
							roundHopId = Integer.parseInt(parts[i]);
							if (!kHops.containsKey(roundHopId)
									&& nodeId != roundHopId) {
								kHops.put(roundHopId, round + 1);
							}
						}
					}
					in++;
				}
				message = "";
				if (lastRound == true)
					break;
			}
			
			// Populating the result into another hashmap for printing the
						// output in prescribed format.
						
			String value = null;
			for (Entry<Integer, Integer> entry : kHops.entrySet()) {
				if ((value = groupHop.get(entry.getValue())) != null)
					groupHop.put(entry.getValue(),
							value + "," + String.valueOf(entry.getKey()));
				else
					groupHop.put(entry.getValue(),
							String.valueOf(entry.getKey()));

			}
			
			// Printing the output
			System.out.println(" Node_id   Hop#   Neighbours");
			System.out.println("----------------------------");
			System.out.format("%4s", nodeId);
			for (Entry<Integer, String> entry : groupHop.entrySet()) {
				if (entry.getKey() == 1)
					System.out.format("%9s        %s \n", entry.getKey(),
							entry.getValue());
				else
					System.out.format("%13s        %s \n", entry.getKey(),
							entry.getValue());
			}
		} catch (ConnectException e) {
			System.out
					.println("Exception caught when trying to listen on port "
							+ portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
		s.close();
	}
}

class Host { 
    String hostname; 
    int port; 
 
    public Host(String hostname, int port) { 
        super(); 
        this.hostname = hostname; 
        this.port = port; 
    } 
}     

