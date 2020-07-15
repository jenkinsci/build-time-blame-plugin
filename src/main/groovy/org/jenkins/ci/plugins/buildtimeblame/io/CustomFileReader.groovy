//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

import java.util.stream.Stream
import java.util.stream.StreamSupport

class CustomFileReader {
    public static Stream<String> eachLineOnlyLF(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream)
                .useDelimiter('\n')
        Iterator<String> iterator = new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return scanner.hasNext()
            }

            @Override
            public String next() {
                return scanner.next().trim()
            }
        }

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL),
                false
        )
    }
}
