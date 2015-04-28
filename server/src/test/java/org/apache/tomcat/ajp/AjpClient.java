/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.ajp;

import org.apache.coyote.ajp.Constants;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;

/**
 * Ported from https://github.com/kohsuke/ajp-client
 */
public class AjpClient implements AutoCloseable {

  private static final int AJP_PACKET_SIZE = 8192;
  private static final byte[] AJP_CPING;

  static {
    final ClientMessage ajpCping = new ClientMessage(16);
    ajpCping.reset();
    ajpCping.appendByte(Constants.JK_AJP13_CPING_REQUEST);
    ajpCping.end();
    AJP_CPING = new byte[ajpCping.getLen()];
    System.arraycopy(ajpCping.getBuffer(), 0, AJP_CPING, 0, ajpCping.getLen());
  }

  private final String host;
  private final int port;
  private Socket socket = null;

  public AjpClient(final InetAddress address, final int port) {
    this.host = address.getHostName();
    this.port = port;
  }

  public AjpClient connect() throws IOException {
    assert socket == null : "already connected";
    socket = SocketFactory.getDefault().createSocket(host, port);
    return this;
  }

  @Override
  public void close() throws IOException {
    socket.close();
    socket = null;
  }

  /**
   * Tests the connection to the server and returns the CPONG response.
   */
  public ClientMessage cping() throws IOException {
    // Send the ping message
    socket.getOutputStream().write(AJP_CPING);
    // Read the response
    return readMessage();
  }

  /**
   * Sends an ClientMessage to the server and returns the response message.
   */
  public ClientMessage get(final URI target) throws IOException {
    final ClientMessage request = createGet(target.toString());
    socket.getOutputStream().write(request.getBuffer(), 0, request.getLen());
    return readMessage();
  }

  /**
   * Create a message to request the given URL.
   */
  private ClientMessage createGet(String url) {
    ClientMessage message = new ClientMessage(AJP_PACKET_SIZE);
    message.reset();

    // Set the header bytes
    message.getBuffer()[0] = 0x12;
    message.getBuffer()[1] = 0x34;

    // Code 2 for forward request
    message.appendByte(Constants.JK_AJP13_FORWARD_REQUEST);

    // HTTP method, GET = 1
    message.appendByte(1);

    // Protocol
    message.append("https");

    // Request URI
    message.append(url);

    // Remote address
    message.append("10.0.0.1");

    // Remote host
    message.append("client.dev.local");

    // Server name
    message.append(host);

    // Port
    message.appendInt(port);

    // Is ssl
    message.appendByte(0x00);

    return message;
  }

  /**
   * Reads a message from the server.
   */
  private ClientMessage readMessage() throws IOException {
    final InputStream is = socket.getInputStream();
    final ClientMessage message = new ClientMessage(AJP_PACKET_SIZE);

    byte[] buf = message.getBuffer();
    int headerLength = message.getHeaderLength();

    read(is, buf, 0, headerLength);

    int messageLength = message.processHeader(false);
    if (messageLength < 0) {
      throw new IOException("Invalid AJP message length");
    } else if (messageLength == 0) {
      return message;
    } else {
      if (messageLength > buf.length) {
        throw new IllegalArgumentException(
            "Message too long [" + messageLength +
            "] for buffer length [" + buf.length + "]");
      }
      read(is, buf, headerLength, messageLength);
      return message;
    }
  }

  private boolean read(InputStream is, byte[] buf, int pos, int n)
      throws IOException {

    int read = 0;
    int res = 0;
    while (read < n) {
      res = is.read(buf, read + pos, n - read);
      if (res > 0) {
        read += res;
      } else {
        throw new IOException("Read failed");
      }
    }
    return true;
  }
}
