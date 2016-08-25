package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class checkers implements Runnable {
private BufferedReader reader;
private PrintWriter writer;
private Message message;
private Socket socket;
private ObjectMapper mapper;
	public checkers(BufferedReader reader,PrintWriter writer, Message message, Socket socket) {
		this.reader= reader;
		this.writer=writer;
		this.message=message;
		this.socket=socket;
		this.mapper= new ObjectMapper();
	}
	public void run(){
		
		String raw;
		try {
			while(!socket.isClosed()){
			raw = reader.readLine();
			message = mapper.readValue(raw, Message.class);
			mapper.writeValue(writer,message);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		}
	}
	

