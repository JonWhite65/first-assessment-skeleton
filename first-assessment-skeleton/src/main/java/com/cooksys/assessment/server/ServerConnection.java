package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerConnection implements Runnable {
	private Message message;
	private ObjectMapper mapper = new ObjectMapper();

	public ServerConnection(Message message) {
		this.message = message;
	}

	public void run() {

		try  
		{
			this.message.setCommand("connect");
			this.message.setUsername("Monster");
			Socket so = new Socket(this.message.getJoiner(), 8080); 
			this.message.setCommand("connect");
			this.message.setUsername("Monster");
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(so.getOutputStream()));
			writer.write(mapper.writeValueAsString(message));
			Socket s = new Socket("localhost", 8080);
			PrintWriter writer1 = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
			this.message.setUsername("Monster1");
			writer1.write(mapper.writeValueAsString(message));
			Scanner sc = new Scanner(so.getInputStream());
			Scanner sc1 = new Scanner(s.getInputStream());
			
			this.message.setUsername("Monster1");
			
			while (!so.isClosed() && !s.isClosed()) {
				if (sc.hasNext()) {
					writer.write(sc.next());
				}
				if (sc1.hasNext()) {
					writer1.write(sc1.next());
				}
			}
		} catch (Exception e) {
		}

	}

}
