package com.cooksys.assessment.server;

import java.awt.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	// keeps a current copy of all client handlers
	public static ArrayList<ClientHandler> clients = new ArrayList<ClientHandler>();
	public static ArrayList<PrintWriter> clientsPw = new ArrayList<PrintWriter>();
	public static ArrayList<String> clientsUsers = new ArrayList<String>();

	// individual print writers intentionally did not include get or set pw
	// available through static ArrayList
	private PrintWriter writer;

	public PrintWriter getWriter() {
		return writer;
	}

	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}

	private ObjectMapper mapper = new ObjectMapper();
	private String user;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	// used as a lock on new client registration
	public static Object a = new Object();

	public ObjectMapper getMapper() {
		return mapper;
	}

	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}

	// potential to write out of order or to the same stream simultaneously if
	// not restricted
	synchronized public void ableToWrite(ArrayList<PrintWriter> a, Message message) {
		for (PrintWriter printer : a) {
			try {
				printer.write(this.mapper.writeValueAsString(message));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			printer.flush();
		}
	}

	synchronized public void ableToWrite(PrintWriter pw, Message message) {

		try {
			System.out.println(message.getUserList());
			pw.write(this.mapper.writeValueAsString(message));
		} catch (IOException e) {
			e.printStackTrace();
		}
		pw.flush();
	}



	public void run() {
		try {

			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			// lock used to avoid to clients registering concurrently

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				switch (message.getCommand()) {
				case "connect":
					boolean duplicate=true;
					synchronized (this) {
						clients.add(this);
						clientsPw.add(this.writer);
						this.user = message.getUsername();
						if(!clientsUsers.contains(this.user)){
						clientsUsers.add(this.user);
						duplicate =false;
						}
					}
					log.info("user <{}> connected", message.getUsername());
					message.setContents("has connected");
					message.setTime(LocalDateTime.now().toString());
					this.ableToWrite(ClientHandler.clientsPw, message);
					if(!duplicate){
					break;
					}
				case "disconnect":
					log.info("user <{}> disconnected", message.getUsername());
					synchronized (this) {
						message.setContents("has disconnected");
						message.setTime(LocalDateTime.now().toString());
						ableToWrite(ClientHandler.clientsPw, message);
						clients.remove(this);
						clientsPw.remove(this.writer);
						clientsUsers.remove(this.user);
						
					}
					this.socket.close();
					break;
				case "echo":
					
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					if(!message.getTime().equals(""))
					{
						message.setContents(LocalDateTime.now().toString()+" <"+message.getUsername()+"> (echo): "+message.getContents());
					}
					else{
					message.setTime(LocalDateTime.now().toString());
					}
					this.ableToWrite(this.writer, message);
					break;
				case "broadcast":
					if(!message.getTime().equals(""))
					{
						message.setContents(LocalDateTime.now().toString()+" <"+message.getUsername()+"> (all): "+message.getContents());
					}
					else{
					message.setTime(LocalDateTime.now().toString());
					}
					ableToWrite(ClientHandler.clientsPw, message);
					break;
				case "users":

					synchronized (this) {
						String[] array = new String[1];
						array= clientsUsers.toArray(array);
						for (String x : array) {
							message.setUserList( message.getUserList()+" "+x);
						}

					}
					if(!message.getTime().equals(""))
					{
						message.setContents(LocalDateTime.now().toString()+": currently connected users: "+ message.getUserList());
					}
					else{
					message.setTime(LocalDateTime.now().toString());
					}
					
					ableToWrite(this.writer, message);
					break;
				default:

					if (message.getCommand().charAt(0) != ('@')) {
						break;
					} else {
						String user = message.getCommand().substring(1);
						boolean foundUser=false;
						for (ClientHandler x : clients) {
							if (x.getUser().equals(user)) {
								System.out.println(message.getTime());
								if(!message.getTime().equals(""))
								{
									message.setContents(LocalDateTime.now().toString()+" <"+message.getUsername()+"> (whisper): "+message.getContents());
								}
								else{
								message.setTime(LocalDateTime.now().toString());
								}
								
								ableToWrite(x.getWriter(), message);
								foundUser=true;
							}
						}
						if(!foundUser){
							message.setCommand("error");
							message.setContents("there is no user by this name");
							ableToWrite(this.writer,message);
						}
					}
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
