package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServerConnection implements Runnable {
	private Message message;
	private ObjectMapper mapper = new ObjectMapper();
	private ExecutorService executor;
	private ClientHandler handler;
	public ServerConnection(Message message, ClientHandler handler) {
		this.message = message;
		this.executor=handler.getExecutor();
		this.handler=handler;
	}

	public void run() {

		try  
		{
			Message tempMessage=new Message();
			tempMessage.setCommand("connect");
			tempMessage.setUsername("Monster");
			Socket so = new Socket(this.message.getJoiner(), 8080); 
			BufferedReader reader = new BufferedReader(new InputStreamReader(so.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(so.getOutputStream()));
			mapper.writeValue(writer,tempMessage);
			boolean connection= true;
			while(connection){
				for (ClientHandler x : handler.clients) {
					if (x.getUser().equals("Monster")) {
						connection = false;
						so=x.getSocket();
					}
				}
				
			}
			
			
			checkers checker1=new checkers(reader,writer,tempMessage,so);
			executor.execute(checker1);
			Socket s = new Socket("localhost", 8080);

			BufferedReader reader1 = new BufferedReader(new InputStreamReader(s.getInputStream()));

			PrintWriter writer1 = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));

			tempMessage.setUsername("Monster1");

			mapper.writeValue(writer1,tempMessage);
//			 connection= true;
//			while(connection){
//				for (ClientHandler x : handler.clients) {
//					if (x.getUser().equals("Monster1")) {
//						connection = false;
//						s=x.getSocket();
//					}
//				}
//				
//			}

			checkers checker2=new checkers(reader1,writer1,tempMessage,s);

			executor.execute(checker2);
		} catch (Exception e) {
		}

	}

}
