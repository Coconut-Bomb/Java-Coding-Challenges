import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.*;

public class BBInterpreter {

    static Integer instructionPointer = 0;
    static List<List<Integer>> stack = new ArrayList<List<Integer>>();
    static Vars vars = new Vars();
    static ArrayList<String> instructionArray;
    static HashMap<String, ArrayList<Integer>> subroutinesMap = new HashMap<String, ArrayList<Integer>>();
    static Boolean inElse = false;

    static void execute() throws Exception {
        Scanner sc = new Scanner(System.in);

        while(instructionPointer < instructionArray.size()){


            String[] instructionParts = instructionArray.get(instructionPointer).trim().split(" ");
            String instructionType = instructionParts[0];
            String varName = null;
            try {
                varName = instructionParts[1]; // Exspression if while is used
            }catch(Exception ArrayIndexOutOfBoundsException){
            }
            //printInstruction(instructionParts);
            System.out.println("INSTRUCTION: " + instructionPointer + " " + instructionType);
            try {
                switch (instructionType) {
                    case "clear":
                        vars.setVar(varName, 0);
                        break;
                    case "incr":
                        vars.incr(varName);
                        break;

                    case "decr":
                        vars.decr(varName);
                        break;

                    case "while":
                        if (elavuateStatment(instructionParts[1], instructionParts[2], instructionParts[3])) {
                            try {
                                if (!(stack.get(stack.size() - 1).get(0).equals(instructionPointer))) {
                                    constructStackFrame();
                                }
                            } catch (Exception IndexOutOfBoundsException) {
                                constructStackFrame();
                            }

                        } else {
                            try {
                                instructionPointer = stack.get(stack.size() - 1).get(1);
                                deconstructStackFrame();
                            }catch (Exception e){
                                skipBlock("while");
                            }
                        }
                        break;

                    case "end":
                        if(!inElse) {
                            instructionPointer = stack.get(stack.size() - 1).get(0) - 1;
                        }else{
                            inElse = false;
                        }
                        break;
                    case "if":
                        // if (if statment) is false skip to else. ++ takes it over
                        if (!elavuateStatment(instructionParts[1], instructionParts[2], instructionParts[3])) {
                            skipBlock("if");
                            inElse = true;
                        }
                        break;
                    case "else": // skip to end of else (if statment was true) ++ takes it over
                        skipBlock("else");
                        break;
                    case "add": // add X Y Z
                        vars.setVar(instructionParts[3], (vars.getVar(instructionParts[1]) + vars.getVar(instructionParts[2])));
                        break;
                    case "sub": // sub X Y Z
                        vars.setVar(instructionParts[3], (vars.getVar(instructionParts[1]) - vars.getVar(instructionParts[2])));
                        break;
                    case "div":
                        vars.setVar(instructionParts[3], (vars.getVar(instructionParts[1]) / vars.getVar(instructionParts[2])));
                        break;
                    case "mod":
                        vars.setVar(instructionParts[3], (vars.getVar(instructionParts[1]) % vars.getVar(instructionParts[2])));
                        break;
                    case "muli":
                        vars.setVar(instructionParts[3], (vars.getVar(instructionParts[1]) * vars.getVar(instructionParts[2])));
                        break;
                    case "exp":
                        vars.setVar(instructionParts[3], (int) Math.pow(vars.getVar(instructionParts[1]), vars.getVar(instructionParts[2])));
                        break;
                    case "print":
                        System.out.println( instructionParts[1] + " : " + vars.getVar(instructionParts[1]) );
                        break;
                    case "def":
                        createSubroutine();
                        System.out.println(subroutinesMap.keySet());
                        break;
                    case "call":
                        callSubroutine();
                        break;
                    default:
                        System.out.println("Unknown Instruction ");
                }
            }catch(Exception e){
                throw new Exception("Invalid  instruction");
            }
            Vars.checkVars();
            vars.printVars();
            instructionPointer++;
            sc.nextLine();
        }

    }

    static void loadInstructions(ArrayList<String> TempInstructionArray){
        instructionArray = TempInstructionArray;
        //subroutinesArray = TempSubroutines;
    }

    static Boolean elavuateStatment(String statmentPart1, String statmentPart2, String statmentPart3){

        int value1;
        int value2;
        String condition = statmentPart2;
        try{
            value1 = Integer.parseInt(statmentPart1);
        }catch(Exception e){
            value1 = vars.getVar(statmentPart1);
        }

        try{
            value2 = Integer.parseInt(statmentPart3);
        }catch(Exception e){
            value2 = vars.getVar(statmentPart3);
        }
        //System.out.println( value1 + " " + condition + " " + value2);
        switch (condition){
            case "not":
                return (value1 != value2);
            case "<":
                return (value1 < value2);
            case ">":
                return (value1 > value2);
            case "<=":
                return (value1 <= value2);
            case ">=":
                return (value1 >= value2);
            case "==":
                return (value1 == value2);
        }

        System.out.println("Evaluative Statment not known");
        System.exit(0);
        return false;

    }

    static void constructStackFrame() {
        // return address, leap address   (when the leap address is reached the program jumps to return address)
        // in a while return address is automaticlay jumped to when end; is reached. leap address is manually
        // jumped to when the while is false.
        ArrayList<Integer> stackFrame = getStatmentBlockStartEndArray();
        stack.add(stackFrame);
        System.out.println("End of Construct Stack Frame");
        System.out.println(Arrays.toString(stackFrame.toArray()));
    }

    static void deconstructStackFrame(){
        stack.remove(stack.size()-1);
    }

    static void printInstruction(String[] instructionParts){
        for ( String part : instructionParts) {
            System.out.println(part);
        }
    }

    static void skipBlock(String type){

        switch (type) {
            case "while": // While (Skip to the end)
                ArrayList<Integer> whileStatmentStartEnd = getStatmentBlockStartEndArray();
                instructionPointer = whileStatmentStartEnd.get(1);
                break;

            case "if": // If (Skip to next Else)

                int tempInstructionPointer = instructionPointer + 1;
                int numOfIF = 1;
                int numOfELSE = 0;

                while (numOfIF != numOfELSE) {
                    String[] instruction = instructionArray.get(tempInstructionPointer).trim().split(" ");

                    if (numOfELSE > numOfIF) {
                        System.out.println("ERROR If Else Error");
                        System.exit(1);
                    }
                    if (instruction[0].equals("if")){
                        numOfIF++;
                    } else if (instruction[0].equals("else")) {
                        numOfELSE++;
                    }
                    //System.out.println(tempInstructionPointer + " : " + numOfELSE +" : "+ numOfIF);
                    tempInstructionPointer++;

                }
                instructionPointer = tempInstructionPointer - 1;

                break;
            case "else":
                System.out.println("ERRROEE");
                ArrayList<Integer> elseStatmentStartEnd = getStatmentBlockStartEndArray();
                instructionPointer = elseStatmentStartEnd.get(1);
                break;
        }
    }

    static ArrayList<Integer> getStatmentBlockStartEndArray(){
        //System.out.println("Start of Construct Stack Frame");

        ArrayList<Integer> returnArray = new ArrayList<Integer>();

        int start = instructionPointer;
        int tempInstructionPointer = instructionPointer + 1;

        int numOfNeedEnd = 1;
        int numOfEnd = 0;

        //System.out.println(whileStart + " " + tempInstructionPointer + " " + numOfWhile + " " + numOfEnd);

        while (numOfNeedEnd != numOfEnd) {
            //System.out.println("\n While Loop Starting with instruction pointer: " + tempInstructionPointer);
            String[] instruction = instructionArray.get(tempInstructionPointer).trim().split(" ");
            //System.out.println("Instruction is: " + Arrays.toString(instruction));
            String instructionType = instruction[0];
            //System.out.println("Instruction Type is:" + instructionType + ":");


            if (numOfEnd > numOfNeedEnd) {
                System.out.println("ERROR constructStackFrame");
                System.exit(1);
            }
            if (instructionType.equals("while") || instructionType.equals("if")) {
                numOfNeedEnd++;
                //System.out.println("While instruction found: " + numOfWhile);
            } else if (instructionType.equals("end")) {
                numOfEnd++;
                //System.out.println("End instruction found: " + numOfEnd);
            }
            tempInstructionPointer++;
        }

        int end = tempInstructionPointer - 1;


        returnArray.add(start);
        returnArray.add(end);

        return returnArray;

    }

    static void createSubroutine(){
        String[] instructionParts = instructionArray.get(instructionPointer).trim().split(" ");
        String subroutineName = instructionParts[1];
        if(!subroutinesMap.containsKey(subroutineName)) {
            ArrayList<Integer> subroutineBlock = getStatmentBlockStartEndArray();
            subroutinesMap.put(subroutineName, subroutineBlock);
            instructionPointer = subroutineBlock.get(1);
        }else{
            System.out.println("Subroutine Already Defined");
            System.exit(1);
        }
    }

    static void callSubroutine(){
        String[] instructionParts = instructionArray.get(instructionPointer).trim().split(" ");
        String subroutineName = instructionParts[1];
        //System.out.println("CALL SUBROUTINE " + subroutineName);
        if (subroutinesMap.containsKey(subroutineName)){
            stack.add(Collections.singletonList(instructionPointer + 1));
            instructionPointer = subroutinesMap.get(subroutineName).get(0);
        }else{
            System.out.println("Subroutine not found");
            System.exit(1);
        }
    }

}
