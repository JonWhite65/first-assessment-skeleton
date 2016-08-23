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
			if(message.getCommand()=="users"){
				this.mapper.writeValue(pw,message);
			}
			else{
			pw.write(this.mapper.writeValueAsString(message));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		pw.flush();
	}
	// user command only
	// synchronized public void ableToWrite(Message message) {
	// System.out.println("here");
	// try {
	// this.writer.write(this.mapper.writeValueAsString(message));
	// } catch (JsonProcessingException e) {
	// e.printStackTrace();
	// }
	//
	// }

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

					synchronized (this) {
						clients.add(this);
						clientsPw.add(this.writer);
						this.user = message.getUsername();
						clientsUsers.add(this.user);
					}
					log.info("user <{}> connected", message.getUsername());
					message.setCommand("connectionAlert");

					message.setContents("has connected");
					message.setTime(LocalDateTime.now().toString());
					this.ableToWrite(ClientHandler.clientsPw, message);

					break;
				case "disconnect":
					log.info("user <{}> disconnected", message.getUsername());
					message.setCommand("connectionAlert");
					message.setContents("has disconnected");
					message.setTime(LocalDateTime.now().toString());
					ableToWrite(ClientHandler.clientsPw, message);
					this.socket.close();
					break;
				case "echo":
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					message.setTime(LocalDateTime.now().toString());
					this.ableToWrite(this.writer, message);
					break;
				case "broadcast":
					message.setTime(LocalDateTime.now().toString());
					ableToWrite(ClientHandler.clientsPw, message);
					break;
				case "users":

					synchronized (this) {
						String[] array = new String[1];
						message.setUserList(clientsUsers.toArray(array));
						
					}
					
					message.setTime(LocalDateTime.now().toString());
					ableToWrite(this.writer, message);

					break;
				default:
					
					if (message.getCommand().charAt(0) != ('@')) {
						break;
					} else {
						String user = message.getCommand().substring(1);
						for (ClientHandler x : clients) {
							if (x.getUser().equals(user)) {
								message.setTime(LocalDateTime.now().toString());
								ableToWrite(x.getWriter(), message);
							}
						}
					}
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
