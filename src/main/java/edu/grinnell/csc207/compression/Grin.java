package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    private static final int MAGIC_NUMBER = 0x736;

    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to decode
     * @param outfile the file to ouptut to
     * @throws IOException 
     */
    public static void decode (String infile, String outfile) throws IOException {
        BitInputStream in = new BitInputStream(infile);
            BitOutputStream out = new BitOutputStream(outfile);

            int magic = in.readBits(32);
            if (magic != MAGIC_NUMBER) {
                throw new IllegalArgumentException("Not a valid .grin file");
            }

            HuffmanTree tree = new HuffmanTree(in);
            tree.decode(in, out);

            in.close();
            out.close();
    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * @param file the file to read
     * @return a freqency map for the given file
     * @throws IOException 
     */
    public static Map<Short, Integer> createFrequencyMap (String file) throws IOException {
        Map<Short, Integer> freqMap = new HashMap<>();

        BitInputStream in = new BitInputStream(file);
            int bits;
            while ((bits = in.readBits(8)) != -1) {
                short value = (short) bits;
                freqMap.put(value, freqMap.getOrDefault(value, 0) + 1);
            }
            in.close();

        return freqMap;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to encode.
     * @param outfile the file to write the output to.
     * @throws IOException 
     */
    public static void encode(String infile, String outfile) throws IOException {
        Map<Short, Integer> freqMap = createFrequencyMap(infile);
            BitInputStream in = new BitInputStream(infile);
            BitOutputStream out = new BitOutputStream(outfile);

            HuffmanTree tree = new HuffmanTree(freqMap);

            out.writeBits(MAGIC_NUMBER, 32); // write magic number
            tree.serialize(out);             // write serialized tree
            tree.encode(in, out);             // write encoded data

            in.close();
            out.close();
    }

    /**
     * The entry point to the program.
     * @param args the command-line arguments.
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
            return;
        }

        String command = args[0];
        String infile = args[1];
        String outfile = args[2];

        if (command.equals("encode")) {
            encode(infile, outfile);
        } else if (command.equals("decode")) {
            decode(infile, outfile);
        } else {
            System.out.println("Invalid command. Must be encode or decode.");
        }
    }
    
}
