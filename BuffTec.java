import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.lang.System.exit;

public class BuffTec {
    public static void main(String[] args) throws IOException {
        int i = 1;
        boolean all = false;
        String[] techs = {"copyByteArray","BufferedStreamIO","transferTo","direct","not-direct",};

        if (args.length >= 3) {
            String SourceFile = args[0];
            String DestFile = args[1];
            String tech = args[2];
            int buffSize = 16;
            if (args.length == 4)   try {
                buffSize = Integer.parseInt(args[3]);
            } catch (NumberFormatException e){
                System.out.print("Buff size must be an integer");
                exit(1);
            }

            long duration = 0;
            while(i >= 0) {
                switch (tech) {
                    case "not-direct": // 80mb: 100-110ms
                                      // 4K: 0-5ms

                        try {
                            File output = new File(DestFile);
                            output.createNewFile();
                            ReadableByteChannel source = Channels.newChannel(new FileInputStream(SourceFile));
                            WritableByteChannel
                                    dest = Channels.newChannel(new FileOutputStream(DestFile));
                            duration = channelCopy(source, dest, buffSize);
                            source.close();
                            dest.close();
                        } catch (FileNotFoundException e) {
                            System.out.print(e);
                        }

                        break;
                    case "direct": // 80mb: 80-90ms
                                  // 4K: 0ms


                        try {
                            ReadableByteChannel source = Channels.newChannel(new FileInputStream(SourceFile));
                            WritableByteChannel dest = Channels.newChannel(new FileOutputStream(DestFile));
                            duration = channelCopyDirect(source, dest, buffSize);
                            source.close();
                            dest.close();
                        } catch (FileNotFoundException e) {
                            System.out.print(e);
                        }

                        break;
                    case "transferTo": // 80mb: 80-100s
                                      // 4K: 10-16ms

                        try {
                            FileChannel source = new FileInputStream(SourceFile).getChannel();
                            FileChannel dest = new FileOutputStream(DestFile).getChannel();
                            duration = transferTo(source, dest);
                            source.close();
                            dest.close();
                        } catch (FileNotFoundException e) {
                            System.out.print(e);
                        }

                        break;

                    case "BufferedStreamIO":// 80mb: 2500-3000ms
                                           // 4K: 0ms


                        try {
                            BufferedInputStream source = new BufferedInputStream(new FileInputStream(SourceFile));
                            BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(DestFile));
                            duration = BufferedStreamIO(source, dest);
                            source.close();
                            dest.close();
                        } catch (FileNotFoundException e) {
                            System.out.print(e);
                        }

                        break;


                    case "copyByteArray": // 80mb: 200-250ms
                                         // 4K: 5-8ms

                        try {

                            duration = copyByteArray(SourceFile, DestFile);
                            ;
                        } catch (FileNotFoundException e) {
                            System.out.print(e);
                        }


                        break;

                    case "all":
                        i = 4;
                        tech= techs[i];
                        all = true;
                        continue;


                    default: {
                        System.out.print("Invalid technique");
                        exit(1);
                    }

                }
                System.out.println("Method " + tech + " took " + duration + "ms");
                if ((tech.equals("not-direct") || tech.equals("direct") || tech.equals("all"))) {
                   if ( buffSize != 16) System.out.println("( Buffer size: " + buffSize + " )");

                }
                else if ( buffSize != 16 && !all) System.out.println("Buffer size ignored (not applicable");
                i--;
                if (i == -1 || !all) break;
                tech= techs[i];
            }
        }
        else System.out.println("Usage: BuffTec inputFile outputFile technique (or all) buff-size (default is 16)");

    }


    private static long channelCopy(ReadableByteChannel source, WritableByteChannel dest,int buffSize) throws IOException {
        final long startTime = System.currentTimeMillis();
        ByteBuffer buffer = ByteBuffer.allocate (buffSize * 1024);
        while (source.read (buffer) != -1) {
            buffer.flip();
            dest.write (buffer);
            buffer.compact();
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            dest.write (buffer);
        }


        final long duration = System.currentTimeMillis() - startTime;
        return duration;

    }
    private static long channelCopyDirect(ReadableByteChannel source, WritableByteChannel dest,int buffSize) throws IOException {
        final long startTime = System.currentTimeMillis();
        ByteBuffer buffer = ByteBuffer.allocateDirect (buffSize * 1024);
        while (source.read (buffer) != -1) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                dest.write (buffer);
            }
            buffer.clear();
        }

        final long duration = System.currentTimeMillis() - startTime;
        return duration;

    }

    private static long transferTo(FileChannel source, FileChannel dest) throws IOException {
        final long startTime = System.currentTimeMillis();
        source.transferTo(0,source.size(),dest);
        final long duration = System.currentTimeMillis() - startTime;
        return duration;

    }
    private static long BufferedStreamIO(BufferedInputStream source, BufferedOutputStream dest) throws IOException {
        final long startTime = System.currentTimeMillis();
        int i;
        do {
            i = source.read();
            if (i != -1)
                dest.write(i);
        } while (i != -1);
        final long duration = System.currentTimeMillis() - startTime;
        return duration;
    }

    private static long copyByteArray(String source,String dest) throws IOException {
        final long startTime = System.currentTimeMillis();

        byte[] data = Files.readAllBytes(Path.of(source));
        Files.write(Path.of(dest),data);
         long duration = System.currentTimeMillis() - startTime;

        return duration;
    }

}