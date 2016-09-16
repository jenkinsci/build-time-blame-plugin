//  Copyright (c) 2016 Deere & Company
package org.jenkins.ci.plugins.buildtimeblame.io

class CustomFileReader {
    public static void eachLineOnlyLF(InputStream inputStream, Closure closure) {
        Scanner scanner = new Scanner(inputStream);
        scanner.useDelimiter('\n');
        while (scanner.hasNext()) {
            closure.call(scanner.next().trim())
        }
        inputStream.close()
    }
}
