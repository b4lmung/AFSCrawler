/*
 * Copyright 2011 Nakatani Shuyo
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

package com.cybozu.labs.langdetect;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.cybozu.labs.langdetect.util.LangProfile;
import com.cybozu.labs.langdetect.util.TagExtractor;


/**
 * Load Wikipedia's abstract XML as corpus and generate its language profile in JSON format.
 * 
 * @author Nakatani Shuyo
 */
public class GenProfile {

//    private static final Logger logger = LoggerFactory.getLogger(GenProfile.class);

    /**
     * Load Wikipedia abstract database file and generate its language profile
     * @param lang target language name
     * @param file target database file path
     * @return Language profile instance
     */
    public static LangProfile load(String lang, File file) {

        LangProfile profile = new LangProfile(lang);

        try (InputStream is = new BufferedInputStream(new FileInputStream(file))){
          
            TagExtractor tagextractor = new TagExtractor("abstract", 100);

            XMLStreamReader reader = null;
            try {
                XMLInputFactory factory = XMLInputFactory.newInstance();
                reader = factory.createXMLStreamReader(is);
                while (reader.hasNext()) {
                    switch (reader.next()) {
                    case XMLStreamReader.START_ELEMENT:
                        tagextractor.setTag(reader.getName().toString());
                        break;
                    case XMLStreamReader.CHARACTERS:
                        tagextractor.add(reader.getText());
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        tagextractor.closeTag(profile);
                        break;
                    }
                }
            } catch (XMLStreamException e) {
                throw new RuntimeException("Training database file '" + file.getName() + "' is an invalid XML.", e);
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (XMLStreamException e) { /* ignore exception */ }
            }

        } catch (IOException e) {
            throw new RuntimeException("Can't open training database file '" + file.getName() + "'", e);
        } 
        return profile;
    }
}
