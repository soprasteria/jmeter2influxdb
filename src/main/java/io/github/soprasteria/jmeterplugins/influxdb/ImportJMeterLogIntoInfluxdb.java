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

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;

/**
*
* <h1>JMeter results file to Influxdb</h1>
* <p>
* Read a result JMeter file in csv format and 
* import all results in Influxdb Database
* Could also import result file from PerfMon, DbMon and JMXMon
</p>
*
* @author Vincent DABURON - Sopra Steria
*
*/

public class ImportJMeterLogIntoInfluxdb {

	public static final String K_TIMESTAMP = "timeStamp";
	public static final String K_ELASPED = "elapsed";
	public static final String K_LABEL = "label";
	public static final String K_RESPONSECODE = "responseCode";
	public static final String K_RESPONSEMESSAGE = "responseMessage";
	public static final String K_RESPONSEMESSAGELONG = "responseMessageLong";
	public static final String K_THREADNAME = "threadName";
	public static final String K_SUCCESS = "success";
	public static final String K_BYTES = "bytes";
	public static final String K_SENTBYTES = "sentBytes";
	public static final String K_GRPTHREADS = "grpThreads";
	public static final String K_ALLTHREADS = "allThreads";
	public static final String K_LATENCY = "Latency";
	public static final String K_HOSTNAME = "Hostname";
	public static final String K_IDLETIME = "IdleTime";
	public static final String K_CONNECT = "Connect";

	public static final String K_SUCCESS_STATUS = "status";
	public static final String K_STATUS_OK = "OK";
	public static final String K_STATUS_KO = "KO";
	public static final String K_PATH = "path"; // jmeter_file_in path

	public static final String K_JMETER_FILE_IN_OPT = "jmeter_file_in";
	public static final String K_TIMESTAMP_FORMAT_OPT = "timestamp_format";
	public static final String K_DELIMITER_OPT = "delimiter";
	public static final String K_INFLUX_DB_URL_OPT = "influxdb_url";
	public static final String K_INFLUXDB_USER_OPT = "user";
	public static final String K_INFLUXDB_PASSWORD_OPT = "password";
	public static final String K_INFLUXDB_DATABASE_OPT = "database";
	public static final String K_TEST_LABEL_OPT = "label";
	public static final String K_APPLICATION_OPT = "application";
	public static final String K_PARSE_RESPONSEMESSAVE_OPT = "parse_response_message";
	public static final String K_MULTIPLY_VALUE_BY_OPT = "multiply_value_by";

	public static final String K_LEVEL_TRACE_OPT = "trace_level";
	
	public static final int K_WRITE_MODULO = 1000; // write informations to InfluxDB every K_WRITE_MODULO lines

	private static final Logger LOGGER = LogManager.getLogger(ImportJMeterLogIntoInfluxdb.class);

	public static void main(String[] args) {

		System.out.println("Begin main");

		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);

		Options options = createOptions();
		Properties parseProperties = null;

		try {
			parseProperties = parseOption(options, args);
		} catch (ParseException ex) {
			helpUsage(options);
			System.exit(1);
		}

		LOGGER.info("parseProperties = " + parseProperties);

		String jmeterFileIn = "res.csv";

		String jmeter_save_saveservice_timestamp_format = "yyyy-MM-dd HH:mm:ss.SSS";
		String dbName = "jmeter_result_from_csv";
		String testLabel = "tir8";
		char delimiter = ';';
		String application = "gestdoc";
		String influxdbUrl = "http://localhost:8086";
		String influxdbUser = "root";
		String influxdbPassword = "password";
		boolean bParseResponseMessage = false;
		boolean bMultiplyValueBy = false;
		double dMutiplyCoef = 0.001;

		String sTmp = "";

		sTmp = (String) parseProperties.get(K_JMETER_FILE_IN_OPT);
		if (sTmp != null) {
			jmeterFileIn = sTmp;
		}

		sTmp = (String) parseProperties.get(K_TIMESTAMP_FORMAT_OPT);
		if (sTmp != null) {
			jmeter_save_saveservice_timestamp_format = sTmp;
		}

		sTmp = (String) parseProperties.get(K_DELIMITER_OPT);
		if (sTmp != null) {
			if ("\\t".equals(sTmp)) {
				delimiter = '\t';
			} else { // for , or ;
				delimiter = sTmp.charAt(0);
			}
		}

		sTmp = (String) parseProperties.get(K_INFLUX_DB_URL_OPT);
		if (sTmp != null) {
			influxdbUrl = sTmp;
		}

		sTmp = (String) parseProperties.get(K_INFLUXDB_USER_OPT);
		if (sTmp != null) {
			influxdbUser = sTmp;
		}

		sTmp = (String) parseProperties.get(K_INFLUXDB_PASSWORD_OPT);
		if (sTmp != null) {
			influxdbPassword = sTmp;
		}

		sTmp = (String) parseProperties.get(K_INFLUXDB_DATABASE_OPT);
		if (sTmp != null) {
			dbName = sTmp;
		}

		sTmp = (String) parseProperties.get(K_TEST_LABEL_OPT);
		if (sTmp != null) {
			testLabel = sTmp;
		}

		sTmp = (String) parseProperties.get(K_APPLICATION_OPT);
		if (sTmp != null) {
			application = sTmp;
		}
		
		sTmp = (String) parseProperties.get(K_PARSE_RESPONSEMESSAVE_OPT);
		if (sTmp != null) {
			bParseResponseMessage = Boolean.parseBoolean(sTmp);
		}
		
		sTmp = (String) parseProperties.get(K_MULTIPLY_VALUE_BY_OPT);
		if (sTmp != null) {
			dMutiplyCoef = Double.parseDouble(sTmp);
			bMultiplyValueBy = true;
		}



		sTmp = (String) parseProperties.get(K_LEVEL_TRACE_OPT);
		if (sTmp != null) {
			String traceLevel = sTmp;
			if (traceLevel.equalsIgnoreCase("WARN")) {
				Logger.getRootLogger().setLevel(Level.WARN);
			}

			if (traceLevel.equalsIgnoreCase("INFO")) {
				Logger.getRootLogger().setLevel(Level.INFO);
			}

			if (traceLevel.equalsIgnoreCase("DEBUG")) {
				Logger.getRootLogger().setLevel(Level.DEBUG);
			}
		}

		try {
			ImportJMeterLogIntoInfluxdb importJMeterLogIntoInfluxdb = new ImportJMeterLogIntoInfluxdb();
			importJMeterLogIntoInfluxdb.readJMeterFileAndWriteInInfluxDB(jmeterFileIn, jmeter_save_saveservice_timestamp_format, delimiter,
					influxdbUrl, influxdbUser, influxdbPassword, dbName, testLabel, application, bParseResponseMessage, bMultiplyValueBy, dMutiplyCoef);
		} catch (Exception ex) {
			LOGGER.error(ex);
			System.exit(1);
		}
		System.out.println("End main (exit 0)");
		System.exit(0);
	}

	public void readJMeterFileAndWriteInInfluxDB(String jmeterFileIn, String timeFormat, char delimiter,
			String influxdbUrl, String influxdbUser, String influxdbPassword, String dbName, String testLabel,
			String application, boolean bParseResponseMessage, boolean bMultiplyValueBy, double dMutiplyCoef) throws Exception {
		InfluxDB influxDB = InfluxDBFactory.connect(influxdbUrl, influxdbUser, influxdbPassword);

		BatchPoints batchPoints = BatchPoints.database(dbName).tag("testLabel", testLabel)
				.tag("application", application).retentionPolicy("autogen").consistency(ConsistencyLevel.ALL).build();

		String sMeasurement = "jmeter_sampler";

		// timeStamp;elapsed;label;responseCode;responseMessage;threadName;success;bytes;sentBytes;grpThreads;allThreads;Latency;Hostname;IdleTime;Connect
		Reader in = null;

		SimpleDateFormat formaterSimpleDate = new SimpleDateFormat(timeFormat);
		Date lastDate = null;
		Date firstDate = null;
		
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		String pattern = "#.#";
		DecimalFormat decimalFormat = new DecimalFormat(pattern, symbols);
		decimalFormat.setParseBigDecimal(true);


		try {
			in = new FileReader(jmeterFileIn);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().withDelimiter(delimiter)
					.parse(in);

			int lineNumber = 1;

			for (CSVRecord record : records) {

				JMeterSimpleSampler jsSampler = new JMeterSimpleSampler();

				Map<String, String> mapRecord = record.toMap();
				boolean withTimeStamp = false;
				boolean withElasped = false;
				boolean withLabel = false;
				boolean withResponseCode = false;
				boolean withResponseMessageLong = false;
				boolean withThreadName = false;
				boolean withSuccess = false;
				boolean withBytes = false;
				boolean withSentBytes = false;
				boolean withGrpThreads = false;
				boolean withAllThreads = false;
				boolean withLatency = false;
				boolean withHostname = false;
				boolean withIdleTime = false;
				boolean withConnect = false;

				String sTmp = "";

				sTmp = mapRecord.get(K_TIMESTAMP);
				if (sTmp == null) {
					throw new java.lang.IllegalArgumentException(K_TIMESTAMP + " is mandatory");
				} else {
					withTimeStamp = true;
					
					if ("ms".equalsIgnoreCase(timeFormat)) {
						jsSampler.setTimeStamp(new Date(Long.parseLong(sTmp)));
					}
					else {
						jsSampler.setTimeStamp(formaterSimpleDate.parse(sTmp));
					}
					
					lastDate = jsSampler.getTimeStamp();
				}

				sTmp = mapRecord.get(K_ELASPED);
				if (sTmp == null) {
					throw new java.lang.IllegalArgumentException(K_ELASPED + " is mandatory");
				} else {
					withElasped = true;
					long lTmp = Long.parseLong(sTmp);
					if (bMultiplyValueBy) {
						lTmp = (Math.round(lTmp * dMutiplyCoef));
					}
					jsSampler.setElapsed(lTmp);
				}

				sTmp = mapRecord.get(K_LABEL);
				if (sTmp == null) {
					throw new java.lang.IllegalArgumentException(K_LABEL + " is mandatory");
				} else {
					withLabel = true;
					jsSampler.setLabel(sTmp);
				}

				sTmp = mapRecord.get(K_RESPONSECODE);
				if (sTmp == null) {
					throw new java.lang.IllegalArgumentException(K_RESPONSECODE + " is mandatory");
				} else {
					withResponseCode = true;
					int responseCodeTmp = 500;
					
					// pas de responsecode = code 200
					if ("".equals(sTmp)) {
						responseCodeTmp = 200;
						jsSampler.setResponseCode(responseCodeTmp);
					}
					else {
						try {
							responseCodeTmp = Integer.parseInt(sTmp);
							jsSampler.setResponseCode(responseCodeTmp);
						}
						catch (Exception ex) {
							jsSampler.setResponseCode(responseCodeTmp);
							LOGGER.error("Put Response Code Error 500 because i can't parse Integer for ResponseCode :<" + sTmp + ">");
						}
					}
				}
				
				sTmp = mapRecord.get(K_RESPONSEMESSAGE);
				if (sTmp != null && bParseResponseMessage) {
					withResponseMessageLong = true;
					long responseMessageLong = 0;
					
					// pas de responsecode = 0
					if ("".equals(sTmp)) {
						responseMessageLong = 0;
						jsSampler.setResponseMessageLong(responseMessageLong);
					}
					else {
						try {
							// parse the string
							BigDecimal bigDecimal = (BigDecimal) decimalFormat.parse(sTmp);
							responseMessageLong = bigDecimal.longValue();
							if (bMultiplyValueBy) {
								responseMessageLong = (Math.round(responseMessageLong * dMutiplyCoef));
							}
							jsSampler.setResponseMessageLong(responseMessageLong);
						}
						catch (Exception ex) {
							withResponseMessageLong = false;
							jsSampler.setResponseMessageLong(responseMessageLong);
							LOGGER.error("Put Response Message Long format, i can't parse Long for ResponseMessage :<" + sTmp + ">");
						}
					}
				}

				sTmp = mapRecord.get(K_THREADNAME);
				if (sTmp != null) {
					withThreadName = true;
					
					if ("".equals(sTmp)) {
						jsSampler.setThreadName("no_thread_name");
					}
					else {
						jsSampler.setThreadName(sTmp);
					}
				}

				sTmp = mapRecord.get(K_SUCCESS);
				if (sTmp == null) {
					throw new java.lang.IllegalArgumentException(K_SUCCESS + " is mandatory");
				} else {
					withSuccess = true;
					boolean successTmp = false;
					try {
						successTmp = Boolean.parseBoolean(sTmp);
						jsSampler.setSuccess(successTmp);
					}
					catch (Exception ex) {
						jsSampler.setSuccess(successTmp);
						LOGGER.error("Put success to false because i can't parse Boolean for success :<" + sTmp + ">");
					}
				}

				sTmp = mapRecord.get(K_BYTES);
				if (sTmp == null) {
					throw new java.lang.IllegalArgumentException(K_BYTES + " is mandatory");
				} else {
					withBytes = true;
					jsSampler.setBytes(Long.parseLong(sTmp));
				}

				sTmp = mapRecord.get(K_SENTBYTES);
				if (sTmp != null) {
					withSentBytes = true;
					jsSampler.setSentBytes(Long.parseLong(sTmp));
				}

				sTmp = mapRecord.get(K_GRPTHREADS);
				if (sTmp != null) {
					withGrpThreads = true;
					jsSampler.setGrpThreads(Integer.parseInt(sTmp));
				}

				sTmp = mapRecord.get(K_ALLTHREADS);
				if (sTmp != null) {
					withAllThreads = true;
					jsSampler.setAllThreads(Integer.parseInt(sTmp));
				}

				sTmp = mapRecord.get(K_LATENCY);
				if (sTmp != null) {
					withLatency = true;
					jsSampler.setLatency(Integer.parseInt(sTmp));
				}

				sTmp = mapRecord.get(K_HOSTNAME);
				if (sTmp != null) {
					withHostname = true;
					jsSampler.setHostname(sTmp);
				}

				sTmp = mapRecord.get(K_IDLETIME);
				if (sTmp != null) {
					withIdleTime = true;
					jsSampler.setIdleTime(Integer.parseInt(sTmp));
				}

				sTmp = mapRecord.get(K_CONNECT);
				if (sTmp != null) {
					withConnect = true;
					jsSampler.setConnect(Integer.parseInt(sTmp));
				}

				LOGGER.info("jsSampler=" + jsSampler);

				if (lineNumber == 1) {
					Builder pointBuilderEvt = Point.measurement("jmeter_events");
					pointBuilderEvt.time(jsSampler.getTimeStamp().getTime(), TimeUnit.MILLISECONDS);
					firstDate = jsSampler.getTimeStamp();
					pointBuilderEvt.addField("text", testLabel + " started");

					Point pointEvt = pointBuilderEvt.build();
					batchPoints.point(pointEvt);
				}

				Builder pointBuilder = Point.measurement(sMeasurement);
				pointBuilder.time(jsSampler.getTimeStamp().getTime(), TimeUnit.MILLISECONDS);

				pointBuilder.tag(K_PATH, jmeterFileIn);
				
				if (withLabel) {
					pointBuilder.tag(K_LABEL, jsSampler.getLabel());
				}

				if (withHostname) {
					pointBuilder.tag(K_HOSTNAME, jsSampler.getHostname());
				}

				if (withElasped) {
					pointBuilder.addField(K_ELASPED, jsSampler.getElapsed());
				}

				if (withResponseCode) {
					pointBuilder.addField(K_RESPONSECODE, jsSampler.getResponseCode());
				}
				
				if (withResponseMessageLong) {
					pointBuilder.addField(K_RESPONSEMESSAGELONG, jsSampler.getResponseMessageLong());
				}

				if (withThreadName) {
					pointBuilder.addField(K_THREADNAME, jsSampler.getThreadName());
				}

				if (withSuccess) {
					pointBuilder.addField(K_SUCCESS, jsSampler.isSuccess());
					if (jsSampler.isSuccess()) {
						pointBuilder.tag(K_SUCCESS_STATUS, K_STATUS_OK);
					} else {
						pointBuilder.tag(K_SUCCESS_STATUS, K_STATUS_KO);
					}
				}

				if (withBytes) {
					pointBuilder.addField(K_BYTES, jsSampler.getBytes());
				}

				if (withSentBytes) {
					pointBuilder.addField(K_SENTBYTES, jsSampler.getSentBytes());
				}

				if (withGrpThreads) {
					pointBuilder.addField(K_GRPTHREADS, jsSampler.getGrpThreads());
				}

				if (withAllThreads) {
					pointBuilder.addField(K_ALLTHREADS, jsSampler.getAllThreads());
				}

				if (withLatency) {
					pointBuilder.addField(K_LATENCY, jsSampler.getLatency());
				}

				if (withIdleTime) {
					pointBuilder.addField(K_IDLETIME, jsSampler.getIdleTime());
				}

				if (withConnect) {
					pointBuilder.addField(K_CONNECT, jsSampler.getConnect());
				}

				Point point = pointBuilder.build();
				batchPoints.point(point);

				if ((lineNumber % K_WRITE_MODULO) == 0) {
					LOGGER.warn("write batchPoints, lineNumber = " + lineNumber);

					influxDB.write(batchPoints);

					batchPoints = BatchPoints.database(dbName).tag("testLabel", testLabel)
							.tag("application", application).retentionPolicy("autogen")
							.consistency(ConsistencyLevel.ALL).build();
				}
				lineNumber++;
			} /// end for

			if (lineNumber > 1) {
				// add the test ended tag event
				Builder pointBuilderEvt = Point.measurement("jmeter_events");
				pointBuilderEvt.time(lastDate.getTime(), TimeUnit.MILLISECONDS);
				pointBuilderEvt.addField("text", testLabel + " ended");
				Point pointEvt = pointBuilderEvt.build();
				batchPoints.point(pointEvt);
			}

			LOGGER.warn("End write batchPoints, lineNumber = " + lineNumber);
			influxDB.write(batchPoints);

		} catch (Exception ex) {
			LOGGER.error(ex);
			throw ex;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
					// nothing to do
				}
			}

			if (influxDB != null) {
				try {
					influxDB.close();
				} catch (Exception e) {
					// nothing to do
				}
			}
		}
	}

	private static Options createOptions() {
		Options options = new Options();

		Option helpOpt = Option.builder("help").hasArg(false).desc("Help and show parameters").build();

		options.addOption(helpOpt);

		Option jmeterFileInOpt = Option.builder(K_JMETER_FILE_IN_OPT).argName(K_JMETER_FILE_IN_OPT).hasArg(true)
				.required(true).desc("JMeter results csv file in (ex res.csv)").build();
		options.addOption(jmeterFileInOpt);

		Option timestampOpt = Option.builder(K_TIMESTAMP_FORMAT_OPT).argName(K_TIMESTAMP_FORMAT_OPT).hasArg(true)
				.required(true)
				.desc("timestamp format corresponding to jmeter.save.saveservice.timestamp_format in jmeter.properties (ms or Simple Date Format with ms precision)")
				.build();
		options.addOption(timestampOpt);

		Option delimiterOpt = Option.builder(K_DELIMITER_OPT).argName(K_DELIMITER_OPT).hasArg(true).required(true)
				.desc("csv character separator corresponding to jmeter.save.saveservice.default_delimiter in jmeter.properties (usually , or ; or \\t)")
				.build();
		options.addOption(delimiterOpt);

		Option influxdbUrlOpt = Option.builder(K_INFLUX_DB_URL_OPT).argName(K_INFLUX_DB_URL_OPT).hasArg(true)
				.required(true).desc("url to the influxdb (ex : http://localhost:8086)").build();
		options.addOption(influxdbUrlOpt);

		Option influxdbUserOpt = Option.builder(K_INFLUXDB_USER_OPT).argName(K_INFLUXDB_USER_OPT).hasArg(true)
				.required(false).desc("login for influxdb connection").build();
		options.addOption(influxdbUserOpt);

		Option influxdbPassword = Option.builder(K_INFLUXDB_PASSWORD_OPT).argName(K_INFLUXDB_PASSWORD_OPT).hasArg(true)
				.required(false).desc("password for influxdb connection").build();
		options.addOption(influxdbPassword);

		Option influxdbDatabaseNameOpt = Option.builder(K_INFLUXDB_DATABASE_OPT).argName(K_INFLUXDB_DATABASE_OPT)
				.hasArg(true).required(true).desc("influxdb database name (must be CREATED before importation)")
				.build();
		options.addOption(influxdbDatabaseNameOpt);

		Option testLabelNameOpt = Option.builder(K_TEST_LABEL_OPT).argName(K_TEST_LABEL_OPT).hasArg(true).required(true)
				.desc("load test label (ex : test2)").build();
		options.addOption(testLabelNameOpt);

		Option applicationOpt = Option.builder(K_APPLICATION_OPT).argName(K_APPLICATION_OPT).hasArg(true).required(true)
				.desc("application name (ex : myApplication)").build();
		options.addOption(applicationOpt);
		
		Option parseResponseMessageOpt = Option.builder(K_PARSE_RESPONSEMESSAVE_OPT).argName(K_PARSE_RESPONSEMESSAVE_OPT).hasArg(true).required(false)
				.desc("for JMXMon, DBMon file set to true because values are in the reponseMessage field (default false for JMeter csv result file or PerfMon)").build();
		options.addOption(parseResponseMessageOpt);
		
		Option multiplyValueByOpt = Option.builder(K_MULTIPLY_VALUE_BY_OPT).argName(K_MULTIPLY_VALUE_BY_OPT).hasArg(true).required(false)
				.desc("PerfMon values are multiply by 1000 so the multipy value to get the good value is 0.001, ex 4000 x 0.001 = 4 the correct value, set -"+ K_MULTIPLY_VALUE_BY_OPT + " 0.001 for PerfMon results").build();
		options.addOption(multiplyValueByOpt);

		Option niveauTraceOpt = Option.builder(K_LEVEL_TRACE_OPT).argName(K_LEVEL_TRACE_OPT).hasArg(true)
				.required(false)
				.desc("trace level (WARN = defaut, INFO = good trace level, DEBUG = be carefull very verbose)").build();
		options.addOption(niveauTraceOpt);
		return options;
	}

	private static Properties parseOption(Options optionsP, String args[])
			throws ParseException, MissingOptionException {
		Properties properties = new Properties();

		CommandLineParser parser = new DefaultParser();
		// parse the command line arguments

		CommandLine line = parser.parse(optionsP, args);

		if (line.hasOption("help")) {
			properties.setProperty("help", "help value");
			return properties;
		}

		if (line.hasOption(K_JMETER_FILE_IN_OPT)) {
			properties.setProperty(K_JMETER_FILE_IN_OPT, line.getOptionValue(K_JMETER_FILE_IN_OPT));
		}

		if (line.hasOption(K_TIMESTAMP_FORMAT_OPT)) {
			properties.setProperty(K_TIMESTAMP_FORMAT_OPT, line.getOptionValue(K_TIMESTAMP_FORMAT_OPT));
		}

		if (line.hasOption(K_DELIMITER_OPT)) {
			properties.setProperty(K_DELIMITER_OPT, line.getOptionValue(K_DELIMITER_OPT));
		}

		if (line.hasOption(K_INFLUX_DB_URL_OPT)) {
			properties.setProperty(K_INFLUX_DB_URL_OPT, line.getOptionValue(K_INFLUX_DB_URL_OPT));
		}

		if (line.hasOption(K_INFLUXDB_USER_OPT)) {
			properties.setProperty(K_INFLUXDB_USER_OPT, line.getOptionValue(K_INFLUXDB_USER_OPT));
		}

		if (line.hasOption(K_INFLUXDB_PASSWORD_OPT)) {
			properties.setProperty(K_INFLUXDB_PASSWORD_OPT, line.getOptionValue(K_INFLUXDB_PASSWORD_OPT));
		}

		if (line.hasOption(K_INFLUXDB_DATABASE_OPT)) {
			properties.setProperty(K_INFLUXDB_DATABASE_OPT, line.getOptionValue(K_INFLUXDB_DATABASE_OPT));
		}

		if (line.hasOption(K_TEST_LABEL_OPT)) {
			properties.setProperty(K_TEST_LABEL_OPT, line.getOptionValue(K_TEST_LABEL_OPT));
		}

		if (line.hasOption(K_APPLICATION_OPT)) {
			properties.setProperty(K_APPLICATION_OPT, line.getOptionValue(K_APPLICATION_OPT));
		}

		if (line.hasOption(K_PARSE_RESPONSEMESSAVE_OPT)) {
			properties.setProperty(K_PARSE_RESPONSEMESSAVE_OPT, line.getOptionValue(K_PARSE_RESPONSEMESSAVE_OPT));
		}
		
		if (line.hasOption(K_MULTIPLY_VALUE_BY_OPT)) {
			properties.setProperty(K_MULTIPLY_VALUE_BY_OPT, line.getOptionValue(K_MULTIPLY_VALUE_BY_OPT));
		}

		if (line.hasOption(K_LEVEL_TRACE_OPT)) {
			properties.setProperty(K_LEVEL_TRACE_OPT, line.getOptionValue(K_LEVEL_TRACE_OPT));
		}

		return properties;
	}

	private static void helpUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		String footer = "Ex : java -jar jmeter2influxdb-<version>-jar-with-dependencies.jar -" + K_JMETER_FILE_IN_OPT + " res.csv -"
				+ K_TIMESTAMP_FORMAT_OPT + " \"yyyy-MM-dd HH:mm:ss.SSS\" -" + K_DELIMITER_OPT + " \";\" -"
				+ K_INFLUX_DB_URL_OPT + " http://localhost:8086 -" + K_INFLUXDB_USER_OPT + " mylogin -"
				+ K_INFLUXDB_PASSWORD_OPT + " mypassword -" + K_INFLUXDB_DATABASE_OPT + " jmeterdb -" + K_TEST_LABEL_OPT
				+ " \"test2\" -" + K_APPLICATION_OPT + " \"myapplication\" -" + K_PARSE_RESPONSEMESSAVE_OPT + " false -" + K_LEVEL_TRACE_OPT + " WARN";
		formatter.printHelp(120, ImportJMeterLogIntoInfluxdb.class.getName(),
				ImportJMeterLogIntoInfluxdb.class.getName(), options, footer, true);
	}

}
