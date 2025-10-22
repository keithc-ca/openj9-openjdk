/*
 * ===========================================================================
 * (c) Copyright IBM Corp. 2025, 2025 All Rights Reserved
 * ===========================================================================
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * IBM designates this particular file as subject to the "Classpath" exception
 * as provided by IBM in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, see <http://www.gnu.org/licenses/>.
 * ===========================================================================
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This replaces the use of sed to clean properties files in JavaCompilation.gmk.
 */
public class PropertyCleaner {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: PropertyCleaner input-file output-file");
        } else {
            String input = args[0];
            String output = args[1];

            try {
                List<String> properties = readProperties(input);

                // Sort to create a stable output.
                properties.sort(null);
                Files.write(Path.of(output), properties, StandardCharsets.UTF_8);
                return;
            } catch (IOException e) {
                System.err.format("Failed: %s%n", e.getLocalizedMessage());
            }
        }

        System.exit(1);
    }

    private static final Pattern Comment = Pattern.compile("^\\s*#.*$");

    private static final Pattern MissingEscape = Pattern.compile("[^\\\\][:=!]");

    private static List<String> readProperties(String input) throws IOException {
        List<String> properties = new ArrayList<>();

        try (FileReader reader = new FileReader(input, StandardCharsets.UTF_8);
                BufferedReader buffered = new BufferedReader(reader)) {
            StringBuilder output = new StringBuilder();
            for (;;) {
                String line = buffered.readLine();

                if (line == null) {
                    if (!output.isEmpty()) {
                        properties.add(output.toString());
                    }
                    break;
                }

                // Skip empty and comment lines.
                if (line.isEmpty() || Comment.matcher(line).matches()) {
                    continue;
                }

                // Add a backslash before any :, = or ! that does not one already.
                {
                    StringBuilder escaped = new StringBuilder();
                    int start = 0;

                    for (Matcher matcher = MissingEscape.matcher(line);;) {
                        if (matcher.find()) {
                            int end = matcher.end() - 1;

                            escaped.append(line.substring(start, end)).append("\\");
                            start = end;
                        } else {
                            if (start != 0) {
                                line = escaped.toString() + line.substring(start);
                            }
                            break;
                        }
                    }
                }

                // Join lines ending with \ with the next line.
                if (line.endsWith("\\")) {
                    output.append(line.substring(0, line.length() - 1));
                    continue;
                }

                if (!output.isEmpty()) {
                    line = output.append(line).toString();
                    output.setLength(0);
                }

                // Remove leading and trailing white space.
                line = line.trim();

                // Replace the first \= with just =.
                int equals = line.indexOf("\\=");

                if (equals >= 0) {
                    line = line.substring(0, equals) + line.substring(equals + 1);
                }

                properties.add(line);
            }
        }

        return properties;
    }

}
