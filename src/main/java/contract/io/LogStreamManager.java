package contract.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import contract.datastructure.DataStructure;
import contract.io.Communicator.CommunicatorMessage;
import contract.wrapper.AnnotatedVariable;
import contract.wrapper.Header;
import contract.wrapper.Operation;
import contract.wrapper.Root;
import contract.utility.OpParser;
import contract.utility.StructParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A LogStreamManager handles communication between processes, components, and the OS file
 * system. <br>
 * Will ignore streamed messages if the listener is null.
 *
 * @author Richard Sundqvist
 */
public class LogStreamManager implements ComListener {

    /**
     * Set to {@code true} to enable human readable printing of log files. {@code false}
     * by default to increase performance.
     */
    public boolean PRETTY_PRINTING = false;
    private final Gson gson = GsonContructor.build();
    private final Communicator communicator;
    private ComListener listener;
    // Wrapper fields
    private Map<String, DataStructure> dataStructures;
    private List<Operation> operations;
    private Map<String, List<String>> sources;

    /**
     * Creates a new LogStreamManager. Will not unwrap streamed messages if the listener
     * is null.
     *
     * @param agentDescriptor The name of the agent using this LogStreamManager, such as
     * "JavaAnnotationProcessor" or "GUI".
     */
    public LogStreamManager (String agentDescriptor) {
        this(agentDescriptor, false);
    }

    /**
     * Creates a new LogStreamManager. Will not unwrap streamed messages if the listener
     * is null.
     *
     * @param suppressIncoming If {@code true}, most incoming messages will be ignored.
     * @param agentDescriptor The name of the agent using this LogStreamManager, such as
     * "JavaAnnotationProcessor" or "GUI".
     */
    public LogStreamManager (String agentDescriptor, boolean suppressIncoming) {
        communicator = new JGroupCommunicator("LogStreamManager/" + agentDescriptor, this, suppressIncoming);
        dataStructures = new HashMap<String, DataStructure>();
        operations = new ArrayList<Operation>();
        sources = null;
    }

    /**
     * Returns the map of known structures held by this LogStreamManager.
     *
     * @return The list of known structures used by this LogStreamManager.
     */
    public Map<String, DataStructure> getDataStructures () {
        return dataStructures;
    }

    /**
     * Set the map of known variables used by this LogStreamManager.
     *
     * @param newDataStrutures A new map of known variables to be used by this LogStreamManager.
     */
    public void setDataStructures (Map<String, DataStructure> newDataStrutures) {
        dataStructures = newDataStrutures;
    }

    /**
     * Returns the list of operations used by this LogStreamManager.
     *
     * @return The list of operations used by this LogStreamManager.
     */
    public List<Operation> getOperations () {
        return operations;
    }

    /**
     * Set the list of operations used by this LogStreamManager.
     *
     * @param operations A new list of operations to be used by this LogStreamManager.
     */
    public void setOperations (List<Operation> operations) {
        this.operations = operations;
    }

    /**
     * Returns the source map held by this LogStreamManager.
     *
     * @return The source map held by this LogStreamManager.
     */
    public Map<String, List<String>> getSources () {
        return sources;
    }

    /**
     * Set the source map held by this LogStreamManager.
     *
     * @param newSources The new source map to be held by this LogStreamManager.
     */
    public void setSources (Map<String, List<String>> newSources) {
        sources = newSources;
    }

    /**
     * Read a log file from the file specified by {@code filePath}.
     *
     * @param filePath Location of the file to read.
     * @return {@code true} if the log was successfully read. {@code false} otherwise.
     * @throws FileNotFoundException If {@code logFile} could not be opened.
     * @throws JsonSyntaxException   If the file could not be parsed by Gson.
     * @throws JsonIOException       When Gson fails to read {@code logFile}.
     */
    public boolean readLog (String filePath) throws JsonIOException, JsonSyntaxException, FileNotFoundException {
        return readLog(new File(filePath));
    }

    /**
     * Read, unwrap and store data from a JSON log file.
     *
     * @param logFile The file to read.
     * @return {@code true} if the log was successfully read. {@code false} otherwise.
     * @throws FileNotFoundException If {@code logFile} could not be opened.
     * @throws JsonSyntaxException   If the file could not be parsed by Gson.
     * @throws JsonIOException       When Gson fails to read {@code logFile}.
     */
    public boolean readLog (File logFile) throws JsonIOException, JsonSyntaxException, FileNotFoundException {
        Root contractRoot = gson.fromJson(new JsonReader(new FileReader(logFile)), Root.class);
        return unwrap(contractRoot);
    }

    /**
     * Returns the Communicator used by this LogStreamManager.
     *
     * @return The Communicator used by this LogStreamManager.
     */
    public Communicator getCommunicator () {
        return communicator;
    }

    /**
     * Print the operations and header information currently held by this
     * LogStreamManager. Set the public variable {@code PRETTY_PRINTING} to true to enable
     * human-readable output. Will generate a filename automatically on the form
     * YY-MM-DD_HHMMSS.
     *
     * @param targetDir The directory to print the log file.
     */
    public void printLogAutoName (File targetDir) throws FileNotFoundException {
        this.printLog(targetDir.toString(), true);
    }

    /**
     * Print the operations and header information currently held by this
     * LogStreamManager. Set the public variable {@code PRETTY_PRINTING} to true to enable
     * human-readable output. Will generate a filename automatically on the form
     * YY-MM-DD_HHMMSS.
     *
     * @param target The location and file name of the file to print.
     */
    public void printLog (File target) throws FileNotFoundException {
        this.printLog(target.toString(), false);
    }

    /**
     * Print the operations and header information currently held by this
     * LogStreamManager. Set the public variable {@code PRETTY_PRINTING} to true to enable
     * human-readable output. If {@code autoName} is true, a file name on the form
     * "YY-MM-DD_HHMMSS.wrapper" will be generated.
     *
     * @param targetPath The location to print the log file.
     * @param autoName If {@code true} file name will be created automatically.
     */
    public void printLog (String targetPath, boolean autoName) throws FileNotFoundException {
        HashMap<String, AnnotatedVariable> annotatedVariables = new HashMap<>();
        annotatedVariables.putAll(dataStructures);
        Header header = new Header(Header.VERSION_UNKNOWN, annotatedVariables, sources);
        printLog(targetPath, new Root(header, operations), autoName);
    }

    /**
     * Stream the data held by this LogStreamManager using the current Communicator.
     *
     * @return True if data was successfully streamed.
     */
    public boolean streamLogData () {
        HashMap<String, AnnotatedVariable> annotatedVariables = new HashMap<>();
        annotatedVariables.putAll(dataStructures);
        Header header = new Header(Header.VERSION_UNKNOWN, annotatedVariables, null);
        return this.stream(new Root(header, operations));
    }

    /**
     * Stream the data held by this LogStreamManager using the current Communicator, then
     * clear data. Data will be cleared only if successful (this method returns true).
     *
     * @return True if data was successfully streamed.
     */
    public boolean streamAndClearLogData () {
        if (streamLogData()) {
            operations.clear();
            dataStructures.clear();
            sources.clear();
            return true;
        }
        return false;
    }

    /**
     * Stream the given Wrapper using the Communicator carried by this LogStreamManager.
     *
     * @param wrapper The Wrapper to stream.
     * @return True if successful, false otherwise.
     */
    public boolean stream (Root wrapper) {
        return communicator.sendWrapper(wrapper);
    }

    /**
     * Stream the given Wrapper using the Communicator carried by this LogStreamManager.
     *
     * @param operation The Operation to stream.
     * @return True if successful, false otherwise.
     */
    public boolean stream (Operation operation) {
        ArrayList<Operation> operations = new ArrayList<>();
        operations.add(operation);
        return this.stream(new Root(null, operations));
    }

    /**
     * Stream the given JSON string using the Communicator carried by this
     * LogStreamManager.
     *
     * @param json The JSON String to stream.
     * @return True if successful, false otherwise.
     */
    public boolean stream (String json) {
        return communicator.sendString(json);
    }

    /**
     * Stream the given Operation list using the Communicator carried by this
     * LogStreamManager.
     *
     * @param operations The operations to stream.
     * @return True if successful, false otherwise.
     */
    public boolean streamOperations (List<Operation> operations) {
        return this.stream(new Root(null, operations));
    }

    /**
     * Stream the given AnnotatedVariable using the Communicator carried by this
     * LogStreamManager.
     *
     * @param annotatedVariable The Wrapper to stream.
     * @return True if successful, false otherwise.
     */
    public boolean stream (AnnotatedVariable annotatedVariable) {
        HashMap<String, AnnotatedVariable> annotatedVariables = new HashMap<>();
        annotatedVariables.put(annotatedVariable.identifier, annotatedVariable);
        Header header = new Header(Header.VERSION_UNKNOWN, annotatedVariables, null);
        return this.stream(new Root(header, null));
    }

    /**
     * Stream the given Wrapper using the Communicator carried by this LogStreamManager.
     *
     * @param wrappers The Wrappers to stream.
     * @return True if ALL wrappers successfully sent, false otherwise.
     */
    public boolean streamWrappers (List<Root> wrappers) {
        boolean allSuccessful = true;
        for (Root w : wrappers) {
            allSuccessful = allSuccessful && communicator.sendWrapper(w);
        }
        return allSuccessful;
    }

    /**
     * Print a simplified version of the given wrapper.
     *
     * @param targetPath The target path, including file name.
     */
    public void printSimpleLog (String targetPath) throws FileNotFoundException {
        HashMap<String, AnnotatedVariable> annotatedVariables = new HashMap<String, AnnotatedVariable>();
        annotatedVariables.putAll(dataStructures);
        Header header = new Header(Header.VERSION_UNKNOWN, annotatedVariables, null);
        printSimpleLog(targetPath + "simple.log", new Root(header, operations));
    }

    /**
     * Print a simplified version of the given wrapper.
     *
     * @param targetPath The target path, including file name.
     * @param wrapper The wrapper to simplify and print.
     */
    public void printSimpleLog (String targetPath, Root wrapper) throws FileNotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append("This is a simplified version of the log. It sacrifices completeness for readability. + \n\n");
        Collection<AnnotatedVariable> c = wrapper.header.annotatedVariables.values();
        sb.append("Header: " + c.size() + " declared variables.\n");
        int i = 0;
        for (AnnotatedVariable av : c) {
            i++;
            sb.append("\t" + i + ":\t" + av.identifier + " (" + av.rawType + ")\n");
        }
        sb.append("\nBody: " + wrapper.body.size() + " operations.\n");
        i = 0;
        for (Operation op : wrapper.body) {
            i++;
            sb.append("\t" + i + ":\t\t" + op + "\n");
        }
        printString(targetPath, sb.toString());
    }

    /**
     * Print the operations and header container in the wrapper given as argument. Set the
     * public variable PRETTY_PRINTING to true to enable human-readable output.
     *
     * @param targetPath The location to print the log file.
     * @param wrapper The wrapper to convert into a log file.
     * @param autoName if {@code true}, a name will be automatically generated.
     */
    public void printLog (String targetPath, Root wrapper, boolean autoName) throws FileNotFoundException {
        Gson GSON;
        DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd_HHmmss");
        Calendar cal = Calendar.getInstance();
        String fileName = autoName ? File.separator + dateFormat.format(cal.getTime()) + ".wrapper" : "";
        if (PRETTY_PRINTING) {
            GSON = new GsonBuilder().setPrettyPrinting().create();
        } else {
            GSON = gson;
        }
        printString(targetPath + fileName, GSON.toJson(wrapper));
    }

    private void printString (String completePath, String str) throws FileNotFoundException {
        PrintStream out = new PrintStream(new FileOutputStream(completePath));
        out.print(str);
        out.flush();
        out.close();
    }

    /**
     * Unwrap a wrapper, add contents to knownVariables and operations.
     *
     * @param wrapper The wrapper to unwrap.
     * @return True if the wrapper was successfully unwrapped. False otherwise.
     */
    public boolean unwrap (Root wrapper) {
        if (wrapper.header != null) {
            if (wrapper.header.annotatedVariables != null) {
                for (AnnotatedVariable av : wrapper.header.annotatedVariables.values()) {
                    DataStructure ds = StructParser.unpackAnnotatedVariable(av);
                    if (ds == null) {
                        return false;
                    }
                    dataStructures.put(av.identifier, ds);
                }
            }
            sources = wrapper.header.sources;
        }
        if (wrapper.body != null) {
            for (Operation op : wrapper.body) {
                operations.add(OpParser.unpackOperation(op));
            }
        }
        return true;
    }

    /**
     * Unwrap a JSON string and store the contents.
     *
     * @param json THE JSON string to process.
     * @return True if the string was successfully parsed and stored. False otherwise.
     */
    public boolean unwrap (String json) {
        Root w = gson.fromJson(json, Root.class);
        return this.unwrap(w);
    }

    @Override
    public void messageReceived (short messageType) {
        if (listener == null) {
            return;
        }
        // Handle Wrapper messagess
        if (messageType == CommunicatorMessage.WRAPPER) {
            List<Root> wrappers = communicator.getAllQueuedMessages();
            for (Root w : wrappers) {
                if (!unwrap(w)) {
                    return;
                }
            }
            if (listener != null) {
                listener.messageReceived(CommunicatorMessage.WRAPPER);
            }
            // Handle Member info messages.
        } else {
            listener.messageReceived(messageType);
        }
    }

    /**
     * Clear list of operations and known variables. Equivalent to calling
     * clearOperations() and clearKnownVariables().
     */
    public void clearData () {
        sources = null;
        dataStructures.clear();
        operations.clear();
    }

    /**
     * Clear all operations held by this LogStreamManager.
     */
    public void clearOperations () {
        operations.clear();
    }

    /**
     * Clear all known variables held by this LogStreamManager.
     */
    public void clearKnownVariables () {
        dataStructures.clear();
    }

    /**
     * Set the CommunicatorListener which will be notified when this Communicator accepts
     * a message.
     *
     * @param newListener The new CommunicatorListener.
     */
    public void setListener (ComListener newListener) {
        listener = newListener;
    }

    public void close () {
        communicator.close();
    }
}
