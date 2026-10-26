import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
public class Main {
    static HashSet<String> builtin;
    static String currentDirectory=System.getProperty("user.dir");
    public static void main(String[] args) throws Exception {
        Scanner sc=new Scanner(System.in);
        builtin=new HashSet<>();
        builtin.add("type");
        builtin.add("echo");
        builtin.add("exit");
        builtin.add("pwd");
        builtin.add("cd");

        try{
            while(true){
                System.out.print("$ ");
                String input=sc.nextLine();
                List<String> raw_tokens=parseInput(input);

                String outputFile=null;
                String errorFile=null;
                boolean appendOutput=false;
                boolean appendError=false;


                List<String> tokens=new ArrayList<>();
                for(int i=0;i<raw_tokens.size();i++){
                    String tok=raw_tokens.get(i);
                    if(tok.equals(">")||tok.equals("1>")){
                        if(i+1<raw_tokens.size()){
                            outputFile=raw_tokens.get(++i);
                        }
                    }else if(tok.equals(">>")||tok.equals("1>>")){
                        if(i+1<raw_tokens.size()){
                            outputFile=raw_tokens.get(++i);
                            appendOutput=true;
                        }
                    }else if(tok.equals("2>")){
                        if(i+1<raw_tokens.size()){
                            errorFile=raw_tokens.get(++i);
                        }
                    }else if(tok.equals("2>>")){
                        if(i+1<raw_tokens.size()){
                            errorFile=raw_tokens.get(++i);
                            appendError=true;
                        }
                    }else{
                        tokens.add(tok);
                    }
                }


                if(tokens.isEmpty())continue;
                String command=tokens.get(0);
                if(command.equals("exit")){
                    break;
                }else if (builtin.contains(command)) {
                    PrintStream originalOut = System.out;
                    PrintStream originalErr = System.err;
                    PrintStream redirectedOut = null;
                    PrintStream redirectedErr = null;
                    try {
                        if (outputFile != null) {
                            File outFile = resolvePath(outputFile);
                            redirectedOut = new PrintStream(new FileOutputStream(outFile,appendOutput));
                            System.setOut(redirectedOut);
                        }
                        if(errorFile!=null){
                            File outFile=resolvePath(errorFile);
                            redirectedErr=new PrintStream(new FileOutputStream(outFile,appendError));
                            System.setErr(redirectedErr);
                        }
                        if (command.equals("echo")) echo(tokens);
                        else if (command.equals("type")) type(tokens);
                        else if (command.equals("pwd")) pwd();
                        else if (command.equals("cd")) cd(tokens);
                    } finally {
                        if (redirectedOut != null) {
                            System.out.flush();
                            System.setOut(originalOut);
                            redirectedOut.close();
                        }
                        if (redirectedErr != null) {
                            System.err.flush();
                            System.setErr(originalErr);
                            redirectedErr.close();
                        }
                    }
                }else{
                    runExternal(tokens,outputFile,errorFile,appendOutput,appendError);
                }
            
            }
        }finally{
            setRawMode(false);
        }

        
        sc.close();
    }
    //REDIRECTING
    private static File resolvePath(String path) {
        if (path.startsWith("/")) {
            return new File(path);
        }
        Path resolved = Paths.get(currentDirectory).resolve(path).normalize();
        return resolved.toFile();
    }

    private static boolean isAllDigits(String s){
        if(s.isEmpty()) return false;
        for(int i=0;i<s.length();i++){
            if(!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    //PARSING
    private static List<String> parseInput(String input){
        List<String> tokens=new ArrayList<>();
        StringBuilder current=new StringBuilder();

        boolean inToken=false;
        boolean inSingleQuote=false;
        boolean isDoubleQuote=false;
        boolean backlash=false;
        

        for(int i=0;i<input.length();i++){
            char c=input.charAt(i);
            if(inSingleQuote){
                if(c=='\''){
                    inSingleQuote=false;
                }else{
                    current.append(c);
                }
            }else if(backlash){
                current.append(c);
                backlash=false;
            }else if(c=='\\'){
                backlash=true;
                inToken=true;
            }else if(isDoubleQuote){
                if(c=='\"'){
                    isDoubleQuote=false;
                }else{
                    current.append(c);
                }
            }else if(c=='\''){
                inSingleQuote=true;
                inToken=true;
           
            }else if(c=='\"'){
                isDoubleQuote=true;
                inToken=true;
            }else if(Character.isWhitespace(c)){
                if(inToken){
                    if(!current.isEmpty())tokens.add(current.toString());
                    current.setLength(0);
                    inToken=false;
                }
            }else if(c=='>'){
                // if(inToken && current.length()>0 && isAllDigits(current.toString())){
                //     current.append(c);
                //     tokens.add(current.toString());
                //     current.setLength(0);
                //     inToken=false;
                // }else{
                //     if(inToken && current.length()>0){
                //         tokens.add(current.toString());
                //         current.setLength(0);
                //         inToken=false;
                //     }
                //     tokens.add(">");
                // }
                String opPrefix = "";
                if(inToken && current.length()>0 && isAllDigits(current.toString())){
                    opPrefix = current.toString();   // e.g. "1" or "2"
                    current.setLength(0);
                    inToken=false;
                }else if(inToken && current.length()>0){
                    tokens.add(current.toString());  // flush a pending word first
                    current.setLength(0);
                    inToken=false;
                }

                StringBuilder op = new StringBuilder(opPrefix);
                op.append('>');
                if(i+1 < input.length() && input.charAt(i+1) == '>'){
                    op.append('>');
                    i++;   // consume the second '>' too, so the outer loop doesn't reprocess it
                }
                tokens.add(op.toString());
            }else{
                current.append(c);
                inToken=true;
            }
            
        }
        if(inToken && !current.isEmpty())tokens.add(current.toString());
        return tokens;
    }
    //AUTO-COMPLETION
    private static void setRawMode(boolean enable) {
        try {
            String[] cmd = enable
                ? new String[]{"/bin/sh","-c","stty raw -echo < /dev/tty"}
                : new String[]{"/bin/sh","-c","stty sane < /dev/tty"};
            new ProcessBuilder(cmd).inheritIO().start().waitFor();
        } catch (Exception e) {
            // If stty isn't available (e.g. no tty attached), just continue;
            // tab completion simply won't have anything to complete against.
        }
    }

    private static String readLineWithCompletion() throws Exception {
        StringBuilder line=new StringBuilder();
        while(true){
            int c=System.in.read();
            if(c==-1){
                return line.length()>0 ? line.toString() : null;
            }else if(c=='\n' || c=='\r'){
                System.out.print("\r\n");
                System.out.flush();
                return line.toString();
            }else if(c=='\t'){
                String completed=completeCommand(line.toString());
                if(completed!=null){
                    String suffix=completed.substring(line.length());
                    System.out.print(suffix);
                    System.out.flush();
                    line.setLength(0);
                    line.append(completed);
                }
                // no match -> do nothing (ignore the tab)
            }else if(c==127 || c==8){
                if(line.length()>0){
                    line.deleteCharAt(line.length()-1);
                    System.out.print("\b \b");
                    System.out.flush();
                }
            }else{
                char ch=(char)c;
                line.append(ch);
                System.out.print(ch);
                System.out.flush();
            }
        }
    }
    private static String completeCommand(String partial){
        if(partial.isEmpty()) return null;
        String[] completions={"echo","exit"};
        List<String> matches=new ArrayList<>();
        for(String candidate:completions){
            if(candidate.startsWith(partial)) matches.add(candidate);
        }
        if(matches.size()==1){
            return matches.get(0)+" ";
        }
        return null;
    }


    //NAVIGATION
    private static void cd(List<String> parts){
        if(parts.size()<2)return;
        String path=parts.get(1);

        if(path.equals("~")){
            String home=System.getenv("HOME");
            if(home==null){
                System.out.println("cd: HOME not set");
                return;
            }
            path=home;
        }else if(path.startsWith("~/")){
            String home=System.getenv("HOME");
            if(home==null){
                System.out.println("cd: HOME not set");
                return;
            }
            path=home+path.substring(1);
        }

        File dir;
        if(path.startsWith("/")){
            dir=new File(path);
        }else{
            Path resolved=Paths.get(currentDirectory).resolve(path).normalize();
            dir=resolved.toFile();
        }

        if(dir.exists() && dir.isDirectory()){
            currentDirectory=dir.getAbsolutePath();
        }else{
            System.out.println("cd: " + path + ": No such file or directory");
        }
    }

    private static void pwd(){
        System.out.println(currentDirectory);
    }


    //BASIC BUILTTIN FEATURES
    private static void type(List<String> parts){
        String enquiry=parts.get(1);
        if(builtin.contains(enquiry)){
            System.out.println(enquiry+" is a shell builtin");
        }else{
            String pathEnv=System.getenv("PATH");
            if(pathEnv==null) pathEnv="";
            boolean notFound=true;
            String dirs[]=pathEnv.split(File.pathSeparator);
            for(String dir:dirs){
                File candidate=new File(dir,enquiry);
                if(candidate.isFile() && candidate.canExecute()){
                System.out.println(enquiry+" is "+candidate.getPath());
                    notFound=false;
                    break;
                }
            }
            if(notFound)System.out.println(enquiry+": not found");
        }
    }

    private static void runExternal(List<String> parts,String outputFile,String errorFile,boolean appendOutput,boolean appendError) throws Exception{
        String command=parts.get(0);
        String pathEnv=System.getenv("PATH");
        if(pathEnv==null) pathEnv="";
        boolean notFound=true;
        String dirs[]=pathEnv.split(File.pathSeparator);
        for(String dir:dirs){
            File candidate=new File(dir,command);
            if(candidate.isFile() && candidate.canExecute()){
                notFound=false;
                break;
            }
        }
        if(notFound){
            System.out.println(command+": command not found");
            return;
        }

        ProcessBuilder pb=new ProcessBuilder(parts);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        // stdout: independent decision, only looks at outputFile
        if (outputFile != null) {
            File outFile = resolvePath(outputFile);
            if (appendOutput) {
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
            } else {
                pb.redirectOutput(ProcessBuilder.Redirect.to(outFile));
            }
        } else {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }

        // stderr: independent decision, only looks at errorFile
        if (errorFile != null) {
            File errFile = resolvePath(errorFile);
            if (appendError) {
                pb.redirectError(ProcessBuilder.Redirect.appendTo(errFile));
            } else {
                pb.redirectError(ProcessBuilder.Redirect.to(errFile));
            }
        } else {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        }
        Process process=pb.start();
        process.waitFor();

    }

    
    private static void echo(List<String> parts){
        StringBuilder newString=new StringBuilder();
        for(int i=1;i<parts.size();i++){
            if(i>1)newString.append(' ');
            newString.append(parts.get(i));
        }
        System.out.println(newString);
    }
}
