package edu.grinnell.csc207.compression;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    private static class Node {
        private short value;
        private int frequency;
        private Node left;
        private Node right;

        public Node(short value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }

        public short getValue() {
            return value;
        }

        public int getFrequency() {
            return frequency;
        }

        public boolean isLeaf(){
            return left == null && right == null;
        }
    }

    private Node root;
    private Map<Short, String> codes;

    /**
     * Constructs a new HuffmanTree from a frequency map.
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {
        // Add EOF
        freqs.put((short) 256, 1); 
        // Sort the map using tree map
        TreeMap<Short, Integer> sorted = new TreeMap<>();
        sorted.putAll(freqs);
        PriorityQueue<Node> queue = new PriorityQueue<>();
        
        for (Short s : sorted.keySet()) {
            Node node = new Node(s, freqs.get(s));
            queue.add(node);
        }

        while (queue.size() > 1) {
            Node left = queue.poll();
            Node right = queue.poll();
            Node parent = new Node((short) 0, left.getFrequency() + right.getFrequency());
            parent.left = left;
            parent.right = right;
            queue.add(parent);
        }

        root = queue.poll();
        codes = new HashMap<>();
        generateCodes(root, "");
    }

    private void generateCodes(Node node, String code) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            codes.put(node.getValue(), code);
            return;
        }
        generateCodes(node.left, code + "0");
        generateCodes(node.right, code + "1");
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree (BitInputStream in) {
        root = readTree(in);
        codes = new HashMap<>();
        generateCodes(root, "");
    }

    private Node readTree(BitInputStream in) {
        int bit = in.readBit();
        if (bit == -1) {
            return null;
        }
        // leaf
        if (bit == 0) {  
            int value = in.readBits(9);  // 9 bits for value
            return new Node((short) value, 0);
        } else {  
            // internal node
            Node left = readTree(in);
            Node right = readTree(in);
            Node parent = new Node((short) 0, 0);
            parent.left = left;
            parent.right = right;
            return parent;
        }
    }

    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize (BitOutputStream out) {
        writeTree(out, root);
    }

    private void writeTree(BitOutputStream out, Node node) {
        if (node.isLeaf()) {
            out.writeBit(0);
            out.writeBits(node.value, 9);
        } else {
            out.writeBit(1);
            writeTree(out, node.left);
            writeTree(out, node.right);
        }
    }
   
    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode (BitInputStream in, BitOutputStream out) {
        int bits;
        while ((bits = in.readBits(8)) != -1) {
            String code = codes.get((short) bits);
            for (char c : code.toCharArray()) {
                out.writeBit(c - '0');  // convert char to int
            }
        }
        // write EOF
        String eofCode = codes.get((short) 256);
        for (char c : eofCode.toCharArray()) {
            out.writeBit(c - '0');
        }
    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode (BitInputStream in, BitOutputStream out) {
        Node node = root;
        while (true) {
            int bit = in.readBit();
            if (bit == -1) break;

            if (bit == 0) {
                node = node.left;
            } else {
                node = node.right;
            }

            if (node.isLeaf()) {
                if (node.value == 256) {  // EOF
                    break;
                } else {
                    out.writeBits(node.value, 8);
                    node = root;
                }
            }
        }
    }
}
