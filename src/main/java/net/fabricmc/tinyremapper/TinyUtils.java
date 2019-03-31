/*
 * Copyright (C) 2016, 2018 Player, asie
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fabricmc.tinyremapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;

import org.objectweb.asm.commons.Remapper;

public final class TinyUtils {
	public static class Mapping {
		public final String owner, name, desc;

		public Mapping(String owner, String name, String desc) {
			this.owner = owner;
			this.name = name;
			this.desc = desc;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || !(other instanceof Mapping)) {
				return false;
			} else {
				Mapping otherM = (Mapping) other;
				return owner.equals(otherM.owner) && name.equals(otherM.name) && desc.equals(otherM.desc);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(owner, name, desc);
		}
	}

	public static class VariableMapping {
		public final String owner, methodName, desc, name;

		public VariableMapping(String owner, String methodName, String desc, String name) {
			this.owner = owner;
			this.methodName = methodName;
			this.desc = desc;
			this.name = name;
		}

		@Override
		public boolean equals(Object other) {
			if (other == null || !other.getClass().equals(VariableMapping.class)) {
				return false;
			} else {
				VariableMapping otherM = (VariableMapping) other;
				return owner.equals(otherM.owner) && methodName.equals(otherM.methodName) && desc.equals(otherM.desc) && name.equals(otherM.name);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(owner, methodName, desc, name);
		}
	}

	private static class SimpleClassMapper extends Remapper {
		final Map<String, String> classMap;

		public SimpleClassMapper(Map<String, String> map) {
			this.classMap = map;
		}

		@Override
		public String map(String typeName) {
			return classMap.getOrDefault(typeName, typeName);
		}
	}

	private TinyUtils() {

	}

	public static IMappingProvider createTinyMappingProvider(final Path mappings, String fromM, String toM) {
		return (classMap, fieldMap, methodMap, variableMap) -> {
			try (BufferedReader reader = getMappingReader(mappings.toFile())) {
				readInternal(reader, fromM, toM, classMap, fieldMap, methodMap, variableMap);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			System.out.printf("%s: %d classes, %d methods, %d fields%n", mappings.getFileName().toString(), classMap.size(), methodMap.size(), fieldMap.size());
		};
	}

	private static BufferedReader getMappingReader(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		if (file.getName().endsWith(".gz")) {
			is = new GZIPInputStream(is);
		}

		return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
	}

	public static IMappingProvider createTinyMappingProvider(final BufferedReader reader, String fromM, String toM) {
		return (classMap, fieldMap, methodMap, variableMap) -> {
			try {
				readInternal(reader, fromM, toM, classMap, fieldMap, methodMap, variableMap);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			System.out.printf("%d classes, %d methods, %d fields%n", classMap.size(), methodMap.size(), fieldMap.size());
		};
	}

	private static void readInternal(BufferedReader reader, String fromM, String toM,
									 Map<String, String> classMap,
									 Map<String, String> fieldMap,
									 Map<String, String> methodMap,
									 Map<String, String> variableMap) throws IOException {
		TinyUtils.read(reader, fromM, toM, (classFrom, classTo) -> {
			classMap.put(classFrom, classTo);
		}, (fieldFrom, fieldTo) -> {
			fieldMap.put(fieldFrom.owner + "/" + fieldFrom.name + ";;" + fieldFrom.desc, fieldTo.owner + "/" + fieldTo.name);
		}, (methodFrom, methodTo) -> {
			methodMap.put(methodFrom.owner + "/" + methodFrom.name + methodFrom.desc, methodTo.owner + "/" + methodTo.name);
		}, (variableFrom, variableTo) -> {
			variableMap.put(variableFrom.owner + "/" + variableFrom.methodName + variableFrom.desc + "#" + variableFrom.name, variableTo);
		});
	}

	public static void read(BufferedReader reader, String from, String to,
							BiConsumer<String, String> classMappingConsumer,
							BiConsumer<Mapping, Mapping> fieldMappingConsumer,
							BiConsumer<Mapping, Mapping> methodMappingConsumer,
							BiConsumer<VariableMapping, String> variableMappingConsumer)
			throws IOException {
		String[] header = reader.readLine().split("\t");
		if (header.length <= 1
				|| !header[0].equals("v1")) {
			throw new IOException("Invalid mapping version!");
		}

		List<String> headerList = Arrays.asList(header);
		int fromIndex = headerList.indexOf(from) - 1;
		int toIndex = headerList.indexOf(to) - 1;

		if (fromIndex < 0) throw new IOException("Could not find mapping '" + from + "'!");
		if (toIndex < 0) throw new IOException("Could not find mapping '" + to + "'!");

		Map<String, String> obfFrom = new HashMap<>();
		Map<String, String> obfTo = new HashMap<>();
		List<String[]> linesStageTwo = new ArrayList<>();

		String line;
		while ((line = reader.readLine()) != null) {
			String[] splitLine = line.split("\t");
			if (splitLine.length >= 2) {
				if ("CLASS".equals(splitLine[0])) {
					classMappingConsumer.accept(splitLine[1 + fromIndex], splitLine[1 + toIndex]);
					obfFrom.put(splitLine[1], splitLine[1 + fromIndex]);
					obfTo.put(splitLine[1], splitLine[1 + toIndex]);
				} else {
					linesStageTwo.add(splitLine);
				}
			}
		}

		SimpleClassMapper descObfFrom = new SimpleClassMapper(obfFrom);
		SimpleClassMapper descObfTo = new SimpleClassMapper(obfTo);

		for (String[] splitLine : linesStageTwo) {
			if ("FIELD".equals(splitLine[0])) {
				String owner = obfFrom.getOrDefault(splitLine[1], splitLine[1]);
				String desc = descObfFrom.mapDesc(splitLine[2]);
				String tOwner = obfTo.getOrDefault(splitLine[1], splitLine[1]);
				String tDesc = descObfTo.mapDesc(splitLine[2]);
				fieldMappingConsumer.accept(
						new Mapping(owner, splitLine[3 + fromIndex], desc),
						new Mapping(tOwner, splitLine[3 + toIndex], tDesc)
				);
			} else if ("METHOD".equals(splitLine[0])) {
				String owner = obfFrom.getOrDefault(splitLine[1], splitLine[1]);
				String desc = descObfFrom.mapMethodDesc(splitLine[2]);
				String tOwner = obfTo.getOrDefault(splitLine[1], splitLine[1]);
				String tDesc = descObfTo.mapMethodDesc(splitLine[2]);
				methodMappingConsumer.accept(
						new Mapping(owner, splitLine[3 + fromIndex], desc),
						new Mapping(tOwner, splitLine[3 + toIndex], tDesc)
				);
			} else if ("VARIABLE".equals(splitLine[0])) {
				String owner = obfFrom.getOrDefault(splitLine[1], splitLine[1]);
				String desc = descObfFrom.mapMethodDesc(splitLine[2]);
				String tOwner = obfTo.getOrDefault(splitLine[1], splitLine[1]);
				String tDesc = descObfTo.mapMethodDesc(splitLine[2]);
				String methodName = splitLine[3];
				variableMappingConsumer.accept(
						new VariableMapping(owner, methodName, desc, splitLine[4 + fromIndex]),
						splitLine[4 + toIndex]
				);
			}
		}
	}
}
