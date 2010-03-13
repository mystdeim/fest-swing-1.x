/*
 * Created on Mar 12, 2010
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright @2010 the original author or authors.
 */
package org.fest.swing.keystroke;

import static org.fest.reflect.core.Reflection.staticField;
import static org.fest.swing.keystroke.KeyStrokeMapping.mapping;
import static org.fest.swing.keystroke.KeyStrokeMappingProvider.NO_MASK;
import static org.fest.util.Strings.concat;
import static org.fest.util.Strings.quote;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;

import org.fest.reflect.exception.ReflectionError;
import org.fest.swing.exception.ParsingException;

/**
 * Understands creation of <code>{@link KeyStrokeMapping}</code>s by parsing a text file.
 *
 * @author Olivier DOREMIEUX
 * @author Alex Ruiz
 *
 * @since 1.2
 */
public class KeyStrokeMappingsParser {

  private static final Map<String, Character> SPECIAL_MAPPINGS = new HashMap<String, Character>();

  static {
    SPECIAL_MAPPINGS.put("COMMA", ',');
  }

  public KeyStrokeMappingProvider parse(String file) {
    List<KeyStrokeMapping> mappings = new ArrayList<KeyStrokeMapping>();
    BufferedReader reader = fileReader(file);
    String line = null;
    try {
      while((line = reader.readLine()) != null)
        mappings.add(mappingFrom(line));
    } catch (IOException e) {
      throw new ParsingException(concat("An I/O error ocurred while parsing file ", file), e);
    }
    return new ParsedKeyStrokeMappingProvider(mappings);
  }

  private BufferedReader fileReader(String file) {
    InputStream fileAsStream = KeyStrokeMapping.class.getClassLoader().getResourceAsStream(file);
    return new BufferedReader(new InputStreamReader(fileAsStream));
  }

  // package-protected for testing
  KeyStrokeMapping mappingFrom(String line) {
    String[] parts = split(line.trim());
    if (parts.length != 3)
      throw new ParsingException(concat("Unable to parse line ", quote(line)));
    char character = characterFrom(parts[0].trim());
    int keyCode = keyCodeFrom(parts[1].trim());
    int modifiers = modifiersFrom(parts[2].trim());
    return mapping(character, keyCode, modifiers);
  }

  private String[] split(String line) {
    return line.split(",");
  }

  private char characterFrom(String s) {
    if (SPECIAL_MAPPINGS.containsKey(s)) return SPECIAL_MAPPINGS.get(s);
    try {
      return s.charAt(0);
    } catch (IndexOutOfBoundsException e) {
      throw new ParsingException(concat("Unable to retrieve character to map from ", quote(s)));
    }
  }

  private int keyCodeFrom(String s) {
    try {
      return staticField(keyCodeNameFrom(s)).ofType(int.class).in(KeyEvent.class).get();
    } catch (ReflectionError e) {
      throw new ParsingException(concat("Unable to retrieve key code from ", quote(s)));
    }
  }

  private String keyCodeNameFrom(String s) {
    return concat("VK_", s);
  }

  private int modifiersFrom(String s) {
    if ("NO_MASK".equals(s)) return NO_MASK;
    try {
      return staticField(s).ofType(int.class).in(InputEvent.class).get();
    } catch (ReflectionError e) {
      throw new ParsingException(concat("Unable to retrieve modifiers from ", quote(s)));
    }
  }
}