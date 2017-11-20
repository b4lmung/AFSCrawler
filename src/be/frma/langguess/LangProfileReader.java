/*
 * Copyright 2011 Francois ROLAND
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.frma.langguess;

import com.cybozu.labs.langdetect.util.LangProfile;

import java.io.*;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads {@link LangProfile}s.
 *
 * @author François ROLAND
 * @author Fabian Kessler
 */
public class LangProfileReader {

	private static final Pattern FREQ_PATTERN = Pattern.compile("\"freq\" ?: ?\\{(.+?)\\}");
	private static final Pattern N_WORDS_PATTERN = Pattern.compile("\"n_words\" ?: ?\\[(.+?)\\]");
	private static final Pattern NAME_PATTERN = Pattern.compile("\"name\" ?: ?\"(.+?)\"");

    /**
     * Reads a {@link LangProfile} from a File in UTF-8.
     */
    public LangProfile read(File profileFile) throws IOException {
        if (!profileFile.exists()) {
            throw new IOException("No such file: "+profileFile);
        } else if (!profileFile.canRead()) {
            throw new IOException("Cannot read file: "+profileFile);
        }
        try (FileInputStream input = new FileInputStream(profileFile)) {
            return read(input);
        }
    }

    /**
     * Reads a {@link LangProfile} from an InputStream in UTF-8.
     */
	public LangProfile read(InputStream inputStream) throws IOException {
		StringBuilder buffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("utf-8")))) {
            String line;
            while((line = reader.readLine()) != null) {
                if (buffer.length() > 0) {
                    buffer.append(' ');
                }
                buffer.append(line);
            }
        }

		String storedProfile = buffer.toString();
		LangProfile langProfile = new LangProfile();

		Matcher m = FREQ_PATTERN.matcher(storedProfile);
		if (m.find()) {
			String[] entries = m.group(1).split(",");
			for (String entry : entries) {
				String[] keyValue = entry.split(":");
				String label = keyValue[0].trim().replace("\"", "");
				langProfile.getFreq().put(label, Integer.valueOf(keyValue[1]));
			}
		}

		m = N_WORDS_PATTERN.matcher(storedProfile);
		if (m.find()) {
			String[] nWords = m.group(1).split(",");
			langProfile.setNWords(new int[nWords.length]);
			for (int i = 0; i < nWords.length; i++) {
				langProfile.getNWords()[i] = Integer.parseInt(nWords[i]);
			}
		}
		
		m = NAME_PATTERN.matcher(storedProfile);
		if (m.find()) {
			langProfile.setName(m.group(1));
		}

		return langProfile;
	}

}
