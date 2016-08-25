package com.cooksys.assessment.server;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;
	// keeps a current copy of all ClientHandlers their PrintWriters and their
	// users
	public static List<ClientHandler> clients = new ArrayList<ClientHandler>();
	public static List<PrintWriter> clientsPw = new ArrayList<PrintWriter>();
	public static List<String> clientsUsers = new ArrayList<String>();
	public static List<InetAddress> clientIp = new ArrayList<InetAddress>();
	// used to transfer Message object back to javaScript
	private PrintWriter writer;
	private ObjectMapper mapper = new ObjectMapper();
	// the user name associated with the connection
	private String user;
	// the writing function checks this variable to determine which method to
	// send
	// type indicates which format of Message the user is expecting
	// determined by a change in default values(specifically of variable time)
	// in the message recieved
	private int type = 0;
	private ExecutorService executor;
	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public PrintWriter getWriter() {
		return writer;
	}

	public void setWriter(PrintWriter writer) {
		this.writer = writer;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public ClientHandler(Socket socket, ExecutorService executor) {
		super();
		this.socket = socket;
		this.executor = executor;

	}

	// Two synchronized write methods used to prevent any thread from using the
	// same writer.

	synchronized public void ableToWrite(List<ClientHandler> a, Message message) {
		for (ClientHandler handel : a) {
			try {
				// sends the correct type of message to the client
				if (handel.getType() == 0) {
					handel.getWriter().write(this.mapper.writeValueAsString(message));
				} else {
					handel.getWriter().write(this.mapper.writeValueAsString(transform(message)));
				}
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			handel.getWriter().flush();
		}
	}

	synchronized public void ableToWrite(ClientHandler cH, Message message) {

		try {
			if (cH.getType() == 0) {
				cH.getWriter().write(this.mapper.writeValueAsString(message));
			} else {
				cH.getWriter().write(this.mapper.writeValueAsString(transform(message)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		cH.getWriter().flush();
	}

	// method for fixing contents field for clients who wish to receive intended
	// output string in contents
	public Message transform(Message a) {
		if (a.getCommand().equals("connect")) {
			a.setContents(a.getTime() + " <" + a.getUsername() + "> " + a.getContents());
		} else if (a.getCommand().equals("disconnect")) {
			a.setContents(a.getTime() + " <" + a.getUsername() + "> " + a.getContents());
		} else if (a.getCommand().equals("echo")) {
			a.setContents(a.getTime() + " <" + a.getUsername() + "> (echo): " + a.getContents());
		} else if (a.getCommand().equals("broadcast")) {
			a.setContents(a.getTime() + " <" + a.getUsername() + "> (all): " + a.getContents());
		} else if (a.getCommand().equals("users")) {
			a.setContents(a.getTime() + ": currently connected users: " + a.getUserList());
		} else if (a.getCommand().charAt(0) != ('@')) {
			a.setContents(a.getTime() + " <" + a.getUsername() + "> (whisper): " + a.getContents());
		} else {
			a.setContents(a.getTime() + " <" + a.getUsername() + "> " + a.getContents());
		}
		return a;
	}

	public void run() {
		try {

			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// made writer a class variable for better access
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				//massive string proof
				if(message.getUsername().length()>1000){
					message.setUsername(message.getUsername().substring(0, 101));
				}
				if(message.getContents().length()>1000){
					message.setContents(message.getContents().substring(0, 101));
				}
				
				// a person might change their name in a second message rewrites
				// in coming messages to have correct user name
				if (!(this.user == null)) {
					message.setUsername(this.user);

				}
				switch (message.getCommand()) {
				case "connect":
					// used to check if a user with the same name is currently
					// logged in to the server
					boolean duplicate = true;
					this.user = message.getUsername();
					// Establishes the type of user
					if (message.getContents().equals("")) {
						this.type = 1;
					}
					// synchronized blocks will be used to ensure the accessing
					// of static ArrayLists are done none concurrently
					synchronized (this) {
						// checks if user is already in server as well as a
						// unique ip(anti-bot defense)
						if (!clientsUsers.contains(this.user)) {
							clients.add(this);
							clientsPw.add(this.writer);
							clientsUsers.add(this.user);
							clientIp.add(this.socket.getInetAddress());
							duplicate = false;
							log.info("user <{}> connected", message.getUsername());
							message.setContents("has connected");
							message.setTime(LocalDateTime.now().toString());
							this.ableToWrite(ClientHandler.clients, message);
						}
					}
					// does not add the new user if userName is taken or if they
					// used a duplicate ip address. sends error message
					if (duplicate) {
						message.setCommand("error");
						message.setContents("user or ip address is already in use");
						message.setTime(LocalDateTime.now().toString());
						this.ableToWrite(this, message);
						this.socket.close();
					}
					break;
				case "disconnect":
					log.info("user <{}> disconnected", message.getUsername());
					synchronized (this) {
						message.setContents("has disconnected");
						message.setTime(LocalDateTime.now().toString());
						ableToWrite(ClientHandler.clients, message);
						clients.remove(this);
						clientsPw.remove(this.writer);
						clientsUsers.remove(this.user);
						clientIp.remove(this.socket.getInetAddress());
					}
					this.socket.close();
					break;
				case "echo":

					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					message.setTime(LocalDateTime.now().toString());
					this.ableToWrite(this, message);
					break;
				case "broadcast":
					log.info("user <{}> broadcast message <{}>", message.getUsername(), message.getContents());
					message.setTime(LocalDateTime.now().toString());
					ableToWrite(ClientHandler.clients, message);
					break;
				case "users":
					log.info("user <{}> users message <{}>", message.getUsername(), message.getContents());
					synchronized (this) {
						String[] array = new String[1];
						array = clientsUsers.toArray(array);
						for (String x : array) {
							message.setUserList(message.getUserList() + " " + x);
						}
					}
					message.setTime(LocalDateTime.now().toString());

					ableToWrite(this, message);
					break;
				case "join":

					ServerConnection a = new ServerConnection(message,this);
					executor.execute(a);

					break;
				default:
					// rejects empty commands and determines message is an @user
					// message
					if (!message.getCommand().equals("") && message.getCommand().charAt(0) != ('@')) {
						message.setCommand("error");
						message.setContents("this was not a recognized command");
						ableToWrite(this, message);
					} else {
						// slices off the @ symbol
						String user = message.getCommand().substring(1);
						boolean foundUser = false;
						// looks for user with name
						for (ClientHandler x : clients) {
							if (x.getUser().equals(user)) {
								message.setTime(LocalDateTime.now().toString());
								ableToWrite(x, message);
								foundUser = true;
								break;
							}
						}
						// no user
						if (!foundUser) {
							message.setCommand("error");
							message.setContents("there is no user by this name");
							ableToWrite(this, message);
						}
					}
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
			// if connection terminated without using disconnect command, system
			// will still behave as if user is in system therefore proceed as a
			// disconnect.
			synchronized (this) {

				Message message = new Message();
				message.setCommand("disconnect");
				message.setUsername(this.user);
				message.setContents("has disconnected");
				message.setTime(LocalDateTime.now().toString());
				ableToWrite(ClientHandler.clients, message);
				clients.remove(this);
				clientsPw.remove(this.writer);
				clientsUsers.remove(this.user);
				clientIp.remove(this.socket.getInetAddress());
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
