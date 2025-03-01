/*
 * ao-messaging-tcp-server - Server for asynchronous bidirectional messaging over TCP sockets.
 * Copyright (C) 2014, 2015, 2016, 2019, 2020, 2021, 2022, 2025  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-messaging-tcp-server.
 *
 * ao-messaging-tcp-server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-messaging-tcp-server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-messaging-tcp-server.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aoapps.messaging.tcp.server;

import com.aoapps.concurrent.Callback;
import com.aoapps.concurrent.Executors;
import com.aoapps.hodgepodge.io.stream.StreamableInput;
import com.aoapps.hodgepodge.io.stream.StreamableOutput;
import com.aoapps.lang.Throwables;
import com.aoapps.messaging.base.AbstractSocketContext;
import com.aoapps.messaging.tcp.TcpSocket;
import com.aoapps.security.Identifier;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server component for bi-directional messaging over TCP.
 */
public class TcpSocketServer extends AbstractSocketContext<TcpSocket> {

  private static final Logger logger = Logger.getLogger(TcpSocketServer.class.getName());

  private static final boolean KEEPALIVE = true;

  private static final boolean SOCKET_SO_LINGER_ENABLED = true;
  private static final int SOCKET_SO_LINGER_SECONDS = 15;

  private static final boolean TCP_NO_DELAY = true;

  private final Executors executors = new Executors();

  private final int port;
  private final int backlog;
  private final InetAddress bindAddr;

  private final Object lock = new Object();
  private ServerSocket serverSocket;

  /**
   * Creates a new TCP socket server.
   */
  public TcpSocketServer(int port) {
    this(port, 50, null);
  }

  /**
   * Creates a new TCP socket server.
   */
  public TcpSocketServer(int port, int backlog) {
    this(port, backlog, null);
  }

  /**
   * Creates a new TCP socket server.
   */
  public TcpSocketServer(int port, int backlog, InetAddress bindAddr) {
    this.port = port;
    this.backlog = backlog;
    this.bindAddr = bindAddr;
  }

  @Override
  @SuppressWarnings("ConvertToTryWithResources")
  public void close() {
    try {
      super.close();
    } finally {
      executors.close();
    }
  }

  /**
   * Starts the I/O of a socket server.  After creation, a socket server does
   * not accept connections until started.  This allows listeners to be
   * registered between creation and start call.
   *
   * @throws IllegalStateException  if closed or already started
   */
  @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch", "AssignmentToCatchBlockParameter", "ThrowableResultIgnored"})
  public void start(
      Callback<? super TcpSocketServer> onStart,
      Callback<? super Throwable> onError
  ) throws IllegalStateException {
    if (isClosed()) {
      throw new IllegalStateException("TcpSocketServer is closed");
    }
    synchronized (lock) {
      if (serverSocket != null) {
        throw new IllegalStateException();
      }
      executors.getUnbounded().submit(() -> {
        try {
          if (isClosed()) {
            throw new SocketException("TcpSocketServer is closed");
          }
          final ServerSocket newServerSocket = new ServerSocket(port, backlog, bindAddr);
          synchronized (lock) {
            TcpSocketServer.this.serverSocket = newServerSocket;
          }
          // Handle incoming messages in a Thread, can try nio later
          executors.getUnbounded().submit(() -> {
            try {
              while (true) {
                synchronized (lock) {
                  // Check if closed
                  if (newServerSocket != TcpSocketServer.this.serverSocket) {
                    break;
                  }
                }
                final Socket socket = newServerSocket.accept();
                final long connectTime = System.currentTimeMillis();
                socket.setKeepAlive(KEEPALIVE);
                socket.setSoLinger(SOCKET_SO_LINGER_ENABLED, SOCKET_SO_LINGER_SECONDS);
                socket.setTcpNoDelay(TCP_NO_DELAY);
                final StreamableInput in = new StreamableInput(socket.getInputStream());
                final StreamableOutput out = new StreamableOutput(socket.getOutputStream());
                final Identifier id = newIdentifier();
                out.writeLong(id.getHi());
                out.writeLong(id.getLo());
                out.flush();
                TcpSocket tcpSocket = new TcpSocket(
                    TcpSocketServer.this,
                    id,
                    connectTime,
                    socket,
                    in,
                    out
                );
                addSocket(tcpSocket);
              }
            } catch (ThreadDeath td) {
              try {
                if (!isClosed()) {
                  callOnError(td);
                }
              } catch (Throwable t) {
                @SuppressWarnings("ThrowableResultIgnored")
                Throwable t2 = Throwables.addSuppressed(td, t);
                assert t2 == td;
              }
              throw td;
            } catch (Throwable t) {
              if (!isClosed()) {
                callOnError(t);
              }
            } finally {
              try {
                close();
              } catch (ThreadDeath td) {
                throw td;
              } catch (Throwable t) {
                logger.log(Level.SEVERE, null, t);
              }
            }
          });
          if (onStart != null) {
            logger.log(Level.FINE, "Calling onStart: {0}", TcpSocketServer.this);
            try {
              onStart.call(TcpSocketServer.this);
            } catch (ThreadDeath td) {
              throw td;
            } catch (Throwable t) {
              logger.log(Level.SEVERE, null, t);
            }
          } else {
            logger.log(Level.FINE, "No onStart: {0}", TcpSocketServer.this);
          }
        } catch (Throwable t0) {
          if (onError != null) {
            logger.log(Level.FINE, "Calling onError", t0);
            try {
              onError.call(t0);
            } catch (ThreadDeath td) {
              t0 = Throwables.addSuppressed(td, t0);
              assert t0 == td;
            } catch (Throwable t2) {
              logger.log(Level.SEVERE, null, t2);
            }
          } else {
            logger.log(Level.FINE, "No onError", t0);
          }
          if (t0 instanceof ThreadDeath) {
            throw (ThreadDeath) t0;
          }
        }
      });
    }
  }
}
