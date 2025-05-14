import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

public class Interpreter {

    public static class Tag {
        public static final int IL = 0;
        public static final int ID = 1;
        public static final int OP = 2;
        public static final int PM = 3;
    }

    public static class Token {
        public final int tag;
        public Token(int t) {
            tag = t;
        }
    }

    public static class IntegerLiteral extends Token {
        public final int value;
        public IntegerLiteral(int v) {
            super(Tag.IL);
            value = v;
        }
    }

    public static class Identifier extends Token {
        public final String name;
        public Identifier(String s) {
            super(Tag.ID);
            name = s;
        }
    }

    public static class Operator extends Token {
        public final char name;
        public Operator(char c) {
            super(Tag.OP);
            name = c;
        }
    }

    public static class PunctuationMark extends Token {
        public final char name;
        public PunctuationMark(char c) {
            super(Tag.PM);
            name = c;
        }
    }

    public static class Lexer {
        private Queue<Character> buffer = new LinkedList<>();
        private Scanner scanner;
        private Hashtable<String, Token> symbols = new Hashtable<>();

        public Lexer(Scanner s) {
            scanner = s;
        }

        private char peek() {
            while (buffer.isEmpty() && scanner.hasNextLine()) {
                String line = scanner.nextLine();
                for (int i = 0; i < line.length(); i++) {
                    buffer.add(line.charAt(i));
                }
                buffer.add('\n'); 
            }

            if (buffer.isEmpty()) {
                return (char) -1;
            }

            return buffer.peek();
        }

        private char next() {
            if (buffer.isEmpty()) {
                return (char) -1;
            }
            return buffer.poll();
        }

        public Token scan() {
            while (Character.isWhitespace(peek())) {
                next();
            }

            char ch = peek();

            if (ch == (char) -1) return null;

            if (ch == '+' || ch == '-' || ch == '*' || ch == '=') {
                next();
                return new Operator(ch);
            }

            if (ch == ';' || ch == '(' || ch == ')') {
                next();
                return new PunctuationMark(ch);
            }

            if (Character.isDigit(ch)) {
                if (ch == '0') {
                    next();
                    if (Character.isDigit(peek())) {
                        throw new RuntimeException("error");
                    }
                    return new IntegerLiteral(0);
                }

                int val = 0;
                while (Character.isDigit(peek())) {
                    val = val * 10 + (next() - '0');
                }
                return new IntegerLiteral(val);
            }

            if (Character.isLetter(ch) || ch == '_') {
                String name = "";
                while (Character.isLetterOrDigit(peek()) || peek() == '_') {
                    name += next();
                }
                if (!symbols.containsKey(name)) {
                    symbols.put(name, new Identifier(name));
                }
                return symbols.get(name);
            }

            throw new RuntimeException("error");
        }
    }

    public static class Parser {
        private Lexer lexer;
        private Token look;
        private Map<String, Integer> memory = new LinkedHashMap<>();

        public Parser(Lexer l) {
            lexer = l;
            move();
        }

        private void move() {
            look = lexer.scan();
        }

        private void error() {
            throw new RuntimeException("error");
        }

        public void program() {
            while (look != null) {
                assignment();
            }

            for (String name : memory.keySet()) {
                int val = memory.get(name);
                System.out.println(name + " = " + val);
            }
        }

        private void assignment() {
            if (!(look instanceof Identifier)) error();
            String varName = ((Identifier) look).name;
            move();

            if (!(look instanceof Operator) || ((Operator) look).name != '=') error();
            move();

            int val = expr();

            if (!(look instanceof PunctuationMark) || ((PunctuationMark) look).name != ';') error();
            move();

            memory.put(varName, val);
        }

        private int expr() {
            int x = term();
            while (look instanceof Operator &&
                  (((Operator) look).name == '+' || ((Operator) look).name == '-')) {
                char op = ((Operator) look).name;
                move();
                int y = term();
                if (op == '+') {
                    x = x + y;
                } else {
                    x = x - y;
                }
            }
            return x;
        }

        private int term() {
            int x = factor();
            while (look instanceof Operator && ((Operator) look).name == '*') {
                move();
                x = x * factor();
            }
            return x;
        }

        private int factor() {
            int sign = 1;

            while (look instanceof Operator) {
                char op = ((Operator) look).name;
                if (op == '-') {
                    sign = -sign;
                    move();
                } else if (op == '+') {
                    move();
                } else {
                    break;
                }
            }

            int val;

            if (look instanceof PunctuationMark && ((PunctuationMark) look).name == '(') {
                move();
                val = expr();
                if (!(look instanceof PunctuationMark) || ((PunctuationMark) look).name != ')') {
                    error();
                }
                move();
            } else if (look instanceof IntegerLiteral) {
                val = ((IntegerLiteral) look).value;
                move();
            } else if (look instanceof Identifier) {
                String name = ((Identifier) look).name;
                if (!memory.containsKey(name)) {
                    error();
                }
                val = memory.get(name);
                move();
            } else {
                error();
                return 0;
            }

            return sign * val;
        }
    }

    public static void main(String[] args) {
        Scanner userInput = new Scanner(System.in);
        System.out.println("Please enter program");
        System.out.println("After entering program, press Enter on empty line to run it.");
        while (true) {
            StringBuilder code = new StringBuilder();
            while (true) {
                String line = userInput.nextLine();
                if (line.isEmpty()) {
                    break;
                }
                code.append(line).append("\n");
            }
            Scanner codeScanner = new Scanner(code.toString());
            Lexer lexer = new Lexer(codeScanner);
            Parser parser = new Parser(lexer);
            try {
                parser.program();
            } catch (RuntimeException e) {
                System.out.println("error");
            }

            System.out.println("Type 'no' to exit, anything else to continue:");
            String input = userInput.nextLine().trim();
            if (input.equalsIgnoreCase("no")) {
                System.out.println("Process end");
                break;
            }
        }
    }
}
