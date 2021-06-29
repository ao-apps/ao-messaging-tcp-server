/*
 * ao-messaging-tcp-server - Server for asynchronous bidirectional messaging over TCP sockets.
 * Copyright (C) 2021  AO Industries, Inc.
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
 * along with ao-messaging-tcp-server.  If not, see <http://www.gnu.org/licenses/>.
 */
module com.aoapps.messaging.tcp.server {
	exports com.aoapps.messaging.tcp.server;
	// Direct
	requires com.aoapps.concurrent; // <groupId>com.aoapps</groupId><artifactId>ao-concurrent</artifactId>
	requires com.aoapps.hodgepodge; // <groupId>com.aoapps</groupId><artifactId>ao-hodgepodge</artifactId>
	requires com.aoapps.lang; // <groupId>com.aoapps</groupId><artifactId>ao-lang</artifactId>
	requires com.aoapps.messaging.base; // <groupId>com.aoapps</groupId><artifactId>ao-messaging-base</artifactId>
	requires com.aoapps.messaging.tcp; // <groupId>com.aoapps</groupId><artifactId>ao-messaging-tcp</artifactId>
	requires com.aoapps.security; // <groupId>com.aoapps</groupId><artifactId>ao-security</artifactId>
	// Java SE
	requires java.logging;
}
