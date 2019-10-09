/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
 
 package io.github.soprasteria.jmeterplugins.influxdb;

import java.util.Date;

public class JMeterSimpleSampler {
	// timeStamp;elapsed;label;responseCode;responseMessage;threadName;success;bytes;sentBytes;grpThreads;allThreads;Latency;Hostname;IdleTime;Connect
	private Date timeStamp;
	private long elapsed;
	private String label;
	private int responseCode;
	
	// use for monitoring like JMXMon or DBMon
	private long responseMessageLong;
	
	private String threadName;
	private boolean success;
	private long bytes;
	
	// since JMeter 3.1 sentBytes
	private long sentBytes;
	
	private int grpThreads;
	private int allThreads;
	private int latency;
	
	private String hostname;
	private int idleTime;
	private int connect;
	
	public JMeterSimpleSampler() {
		
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public long getElapsed() {
		return elapsed;
	}

	public void setElapsed(long elapsed) {
		this.elapsed = elapsed;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	public long getResponseMessageLong() {
		return responseMessageLong;
	}

	public void setResponseMessageLong(long responseMessageLong) {
		this.responseMessageLong = responseMessageLong;
	}
	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public long getBytes() {
		return bytes;
	}

	public void setBytes(long bytes) {
		this.bytes = bytes;
	}

	public long getSentBytes() {
		return sentBytes;
	}

	public void setSentBytes(long sentBytes) {
		this.sentBytes = sentBytes;
	}

	public int getGrpThreads() {
		return grpThreads;
	}

	public void setGrpThreads(int grpThreads) {
		this.grpThreads = grpThreads;
	}

	public int getAllThreads() {
		return allThreads;
	}

	public void setAllThreads(int allThreads) {
		this.allThreads = allThreads;
	}

	public int getLatency() {
		return latency;
	}

	public void setLatency(int latency) {
		this.latency = latency;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}


	public int getIdleTime() {
		return idleTime;
	}

	public void setIdleTime(int idleTime) {
		this.idleTime = idleTime;
	}
	public int getConnect() {
		return connect;
	}

	public void setConnect(int connect) {
		this.connect = connect;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JMeterSimpleSampler [timeStamp=");
		builder.append(timeStamp);
		builder.append(", elapsed=");
		builder.append(elapsed);
		builder.append(", label=");
		builder.append(label);
		builder.append(", responseCode=");
		builder.append(responseCode);
		builder.append(", responseMessageLong=");
		builder.append(responseMessageLong);
		builder.append(", threadName=");
		builder.append(threadName);
		builder.append(", success=");
		builder.append(success);
		builder.append(", bytes=");
		builder.append(bytes);
		builder.append(", sentBytes=");
		builder.append(sentBytes);
		builder.append(", grpThreads=");
		builder.append(grpThreads);
		builder.append(", allThreads=");
		builder.append(allThreads);
		builder.append(", latency=");
		builder.append(latency);
		builder.append(", hostname=");
		builder.append(hostname);
		builder.append(", idleTime=");
		builder.append(idleTime);
		builder.append(", connect=");
		builder.append(connect);
		builder.append("]");
		return builder.toString();
	}
	
}
