// JavaQuizApp.java
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class JavaQuizApp {

    static JFrame loginFrame, sizeFrame, quizFrame;
    static JTextField usernameField;
    static JPasswordField passwordField;
    static String currentUser = "";

    static JTextArea questionArea;
    static JRadioButton[] optionButtons = new JRadioButton[4];
    static ButtonGroup bg = new ButtonGroup();
    static JButton nextBtn, finishBtn, resetBtn;
    static JLabel timerLabel;
    static JComboBox<String> themeToggle;

    static int currentQuestion = 0, quizSize = 10;
    static int[] userAnswers;
    static String[][] questionBank;
    static List<String[]> fullQuestionBank = new ArrayList<>();
    static javax.swing.Timer timer;
    static int timeLeft;

    static Map<String, String> users = new HashMap<>();

    enum Theme { DARK, LIGHT }
    static Theme currentTheme = Theme.LIGHT; // Start with Light theme by default

    public static void main(String[] args) {
        loadQuestions();
        SwingUtilities.invokeLater(JavaQuizApp::showLoginScreen);
    }

    static void applyTheme(Component c) {
        Color bgColor = (currentTheme == Theme.DARK) ? new Color(30, 30, 30) : Color.WHITE;
        Color fg = (currentTheme == Theme.DARK) ? Color.WHITE : Color.BLACK;
        c.setBackground(bgColor);
        if (c instanceof Container) {
            for (Component comp : ((Container) c).getComponents()) {
                if (!(comp instanceof JButton || comp instanceof JTextField || comp instanceof JPasswordField || comp instanceof JComboBox)) {
                    comp.setBackground(bgColor);
                }
                comp.setForeground(fg);
                applyTheme(comp);
            }
        }
    }

    static JComboBox<String> createThemeToggle(Runnable updateUI) {
        JComboBox<String> toggle = new JComboBox<>(new String[]{"Light Mode", "Dark Mode"});
        toggle.setBounds(700, 10, 120, 30);
        toggle.setSelectedIndex(currentTheme == Theme.LIGHT ? 0 : 1);
        toggle.addActionListener(e -> {
            currentTheme = (toggle.getSelectedIndex() == 0) ? Theme.LIGHT : Theme.DARK;
            updateUI.run();
        });
        return toggle;
    }

    static void showLoginScreen() {
        loginFrame = new JFrame("Login / Signup");
        loginFrame.setSize(400, 300);
        loginFrame.setLayout(null);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLocationRelativeTo(null);

        JLabel title = new JLabel("Java Quiz App", JLabel.CENTER);
        title.setBounds(50, 10, 300, 40);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel userLbl = new JLabel("Username:");
        userLbl.setBounds(50, 70, 80, 25);
        usernameField = new JTextField();
        usernameField.setBounds(140, 70, 200, 25);

        JLabel passLbl = new JLabel("Password:");
        passLbl.setBounds(50, 110, 80, 25);
        passwordField = new JPasswordField();
        passwordField.setBounds(140, 110, 200, 25);

        JButton loginBtn = new JButton("Login");
        loginBtn.setBounds(80, 170, 100, 30);
        JButton signupBtn = new JButton("Signup");
        signupBtn.setBounds(200, 170, 100, 30);

        loginBtn.addActionListener(e -> doLogin());
        signupBtn.addActionListener(e -> doSignup());

        loginFrame.add(title);
        loginFrame.add(userLbl);
        loginFrame.add(usernameField);
        loginFrame.add(passLbl);
        loginFrame.add(passwordField);
        loginFrame.add(loginBtn);
        loginFrame.add(signupBtn);

        themeToggle = createThemeToggle(() -> {
            applyTheme(loginFrame);
            loginFrame.repaint();
        });
        loginFrame.add(themeToggle);

        applyTheme(loginFrame);
        loginFrame.setVisible(true);
    }

    static void doLogin() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        String hashed = hash(pass);
        if (users.containsKey(user) && users.get(user).equals(hashed)) {
            currentUser = user;
            loginFrame.dispose();
            showQuestionCountChooser();
        } else {
            JOptionPane.showMessageDialog(loginFrame, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static void doSignup() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        if (users.containsKey(user)) {
            JOptionPane.showMessageDialog(loginFrame, "Username already taken", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            users.put(user, hash(pass));
            JOptionPane.showMessageDialog(loginFrame, "Signup successful! Now login.");
        }
    }

    static void showQuestionCountChooser() {
        sizeFrame = new JFrame("Select Quiz Size");
        sizeFrame.setSize(600, 400);
        sizeFrame.setLayout(new BorderLayout());
        sizeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        sizeFrame.setLocationRelativeTo(null);

        JLabel bg = new JLabel(new ImageIcon("hd-java-programming-logo-png-701751694771848sm650yaqjt.png"));
        bg.setLayout(new GridBagLayout());

        JPanel overlay = new JPanel();
        overlay.setOpaque(true);
        overlay.setBackground(new Color(0, 0, 0, 180));
        overlay.setLayout(new BoxLayout(overlay, BoxLayout.Y_AXIS));
        overlay.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));

        JLabel title = new JLabel("How many questions would you like?");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        overlay.add(title);
        overlay.add(Box.createRigidArea(new Dimension(0, 30)));

        String[] options = {"10 Questions", "15 Questions", "20 Questions"};
        for (String opt : options) {
            JButton btn = new JButton(opt);
            btn.setFont(new Font("Arial", Font.BOLD, 16));
            btn.setMaximumSize(new Dimension(200, 40));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setBackground(Color.DARK_GRAY);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setActionCommand(opt.split(" ")[0]);
            btn.addActionListener(e -> {
                quizSize = Integer.parseInt(e.getActionCommand());
                prepareQuiz();
                sizeFrame.dispose();
                showQuizUI();
            });
            overlay.add(btn);
            overlay.add(Box.createRigidArea(new Dimension(0, 20)));
        }

        themeToggle = createThemeToggle(() -> {
            sizeFrame.dispose();
            showQuestionCountChooser(); // Reload screen with new theme
        });
        overlay.add(themeToggle);

        bg.add(overlay);
        sizeFrame.setContentPane(bg);
        sizeFrame.setVisible(true);
    }

    static void prepareQuiz() {
        if (fullQuestionBank.size() < quizSize) {
            JOptionPane.showMessageDialog(null, "Not enough questions!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Collections.shuffle(fullQuestionBank);
        questionBank = fullQuestionBank.subList(0, quizSize).toArray(new String[quizSize][6]);
        userAnswers = new int[quizSize];
    }

    static void showQuizUI() {
        quizFrame = new JFrame("Quiz - Welcome " + currentUser);
        quizFrame.setSize(1200, 700);
        quizFrame.setLayout(null);
        quizFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        quizFrame.setLocationRelativeTo(null);
        quizFrame.getContentPane().setBackground(new Color(224, 247, 250)); // Light blue background

        questionArea = new JTextArea();
        questionArea.setFont(new Font("Consolas", Font.BOLD, 18));
        questionArea.setBounds(100, 50, 1000, 100);
        questionArea.setEditable(false);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        quizFrame.add(questionArea);

        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setBounds(150, 180 + i * 60, 800, 40);
            optionButtons[i].setFont(new Font("Arial", Font.PLAIN, 18));
            quizFrame.add(optionButtons[i]);
            bg.add(optionButtons[i]);
        }

        timerLabel = new JLabel("Time Left: 20");
        timerLabel.setBounds(1000, 10, 150, 30);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        quizFrame.add(timerLabel);

        nextBtn = new JButton("Next");
        finishBtn = new JButton("Finish");
        resetBtn = new JButton("Reset");

        // Adjusted positions now that Previous is removed
        nextBtn.setBounds(350, 500, 120, 40);
        finishBtn.setBounds(500, 500, 120, 40);
        resetBtn.setBounds(650, 500, 120, 40);

        quizFrame.add(nextBtn);
        quizFrame.add(finishBtn);
        quizFrame.add(resetBtn);

        themeToggle = createThemeToggle(() -> {
            applyTheme(quizFrame);
            quizFrame.repaint();
        });
        themeToggle.setFont(new Font("Arial", Font.PLAIN, 12));
        themeToggle.setBounds(880, 10, 110, 25);
        quizFrame.add(themeToggle);

        timerLabel.setBounds(1000, 10, 150, 30);

        nextBtn.addActionListener(e -> navigate(1));
        finishBtn.addActionListener(e -> endQuiz());
        resetBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(quizFrame, "Reset with new questions?") == JOptionPane.YES_OPTION) {
                prepareQuiz();
                currentQuestion = 0;
                loadQuestion(0);
                restartTimer();
            }
        });

        currentQuestion = 0;
        loadQuestion(0);
        startTimer();
        applyTheme(quizFrame);
        quizFrame.setVisible(true);
    }

    static void navigate(int dir) {
        saveAnswer();
        currentQuestion = Math.max(0, Math.min(quizSize - 1, currentQuestion + dir));
        loadQuestion(currentQuestion);
        restartTimer();
    }

    static void loadQuestion(int idx) {
        questionArea.setText((idx + 1) + ". " + questionBank[idx][0]);
        bg.clearSelection();
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(questionBank[idx][i + 1]);
            optionButtons[i].setSelected(userAnswers[idx] == i + 1);
        }
        // No prevBtn anymore; just control Next
        nextBtn.setEnabled(idx < quizSize - 1);
    }

    static void saveAnswer() {
        for (int i = 0; i < 4; i++) {
            if (optionButtons[i].isSelected()) {
                userAnswers[currentQuestion] = i + 1;
            }
        }
    }

    static void startTimer() {
        timeLeft = 20;
        timerLabel.setText("Time Left: " + timeLeft);
        timer = new javax.swing.Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText("Time Left: " + timeLeft);
            if (timeLeft <= 0) {
                if (currentQuestion < quizSize - 1) {
                    navigate(1);
                } else {
                    endQuiz();
                }
            }
        });
        timer.start();
    }

    static void restartTimer() {
        if (timer != null) timer.stop();
        startTimer();
    }

    static void endQuiz() {
        saveAnswer();
        if (timer != null) timer.stop();
        int score = 0;
        for (int i = 0; i < quizSize; i++) {
            if (userAnswers[i] == Integer.parseInt(questionBank[i][5])) score++;
        }

        JTextPane reviewPane = new JTextPane();
        reviewPane.setContentType("text/html");
        StringBuilder result = new StringBuilder("<html><body style='font-family:sans-serif;'>");
        result.append("<h2>Your Score: ").append(score).append("/").append(quizSize).append("</h2><hr>");
        for (int i = 0; i < quizSize; i++) {
            result.append("<b>Q").append(i + 1).append(":</b> ").append(questionBank[i][0]).append("<br>");
            result.append("Correct Answer: <span style='color:green;'>")
                  .append(questionBank[i][Integer.parseInt(questionBank[i][5])]).append("</span><br>");
            result.append("Your Answer: ").append(
                (userAnswers[i] > 0 && userAnswers[i] == Integer.parseInt(questionBank[i][5])) ?
                "<span style='color:green;'>" + questionBank[i][userAnswers[i]] + "</span>" :
                "<span style='color:red;'>" + (userAnswers[i] > 0 ? questionBank[i][userAnswers[i]] : "No Answer") + "</span>"
            ).append("<br><br>");
        }
        result.append("</body></html>");
        reviewPane.setText(result.toString());
        reviewPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(reviewPane);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        JOptionPane.showMessageDialog(quizFrame, scrollPane, "Review", JOptionPane.INFORMATION_MESSAGE);
        quizFrame.dispose();
    }

    static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    static void loadQuestions() {
        fullQuestionBank.addAll(Arrays.asList(new String[][] {
            {"Which keyword is used to define a subclass?", "extends", "implements", "inherits", "final", "1"},
            {"Which method starts a thread?", "start()", "run()", "execute()", "launch()", "1"},
            {"Which operator checks equality?", "==", "=", "equals", "!=", "1"},
            {"Which loop always executes once?", "while", "for", "do-while", "foreach", "3"},
            {"Which class is a wrapper for int?", "Int", "Integer", "intWrapper", "WrapInt", "2"},
            {"Which access modifier allows access from anywhere?", "private", "public", "protected", "default", "2"},
            {"What is the return type of main?", "void", "int", "String", "char", "1"},
            {"What does JVM stand for?", "Java Variable Machine", "Java Virtual Machine", "Java Verified Mode", "None", "2"},
            {"Which of these is not a Java keyword?", "continue", "goto", "break", "throwing", "4"},
            {"What is the default value of boolean?", "true", "0", "false", "null", "3"},
            {"How to create object in Java?", "new", "create", "make", "build", "1"},
            {"What is polymorphism?", "Multiple forms", "Multiple classes", "Many objects", "Code reuse", "1"},
            {"Which block always executes?", "catch", "try", "finally", "throw", "3"},
            {"Which method reads input from user?", "next()", "read()", "input()", "scan()", "1"},
            {"Which type does not allow duplicates?", "Set", "List", "ArrayList", "LinkedList", "1"},
            {"Which interface allows iteration?", "Iterator", "Iterable", "Collection", "List", "2"},
            {"Which is a marker interface?", "Serializable", "Runnable", "Cloneable", "All of these", "4"},
            {"Which is a valid access modifier?", "package", "internal", "protected", "external", "3"},
            {"Which class is used to format strings?", "Formatter", "String", "Builder", "Text", "1"},
            {"Which operator is used for logical AND?", "&&", "&", "||", "!", "1"},
            {"How many bits in a byte?", "8", "4", "16", "32", "1"},
            {"Which one is not exception?", "IOException", "ClassNotFoundException", "FileException", "NullPointerException", "3"},
            {"Which is superclass of all classes?", "Class", "Main", "Object", "Base", "3"},
            {"What is inheritance?", "Code reuse", "Code hiding", "Code duplication", "None", "1"},
            {"Which is used to handle exceptions?", "try-catch", "for-loop", "scanner", "handler", "1"},
            {"Which data type for true/false?", "boolean", "int", "byte", "float", "1"},
            {"Which class is used to write to file?", "FileWriter", "Scanner", "PrintReader", "Output", "1"},
            {"Which keyword prevents override?", "private", "final", "static", "public", "2"},
            {"Which function to convert string to int?", "Integer.parseInt()", "parse()", "int()", "convert()", "1"},
            {"What is Java?", "Platform", "Language", "Technology", "All", "4"},
            {"How to stop loop in Java?", "exit", "break", "return", "stop", "2"},
            {"What is constructor?", "Special method", "Loop", "Class", "Static method", "1"},
            {"Which method is used to compare strings?", "equals()", "==", "compare", "!=", "1"},
            {"What is garbage collection?", "Free memory", "Clean disk", "Restart JVM", "None", "1"},
            {"Which operator is for OR?", "&&", "||", "!", "&", "2"},
            {"Which keyword is for interface implementation?", "inherit", "extends", "implements", "interface", "3"},
            {"How to handle runtime error?", "catch", "try-catch", "throw", "final", "2"},
            {"What is abstract class?", "Can’t be instantiated", "Must override", "Both", "None", "1"},
            {"Which is not OOP concept?", "Encapsulation", "Abstraction", "Recursion", "Polymorphism", "3"},
            {"Which of the following is a loop?", "for", "while", "do-while", "All", "4"},
            {"Which keyword is used to declare constant?", "const", "final", "static", "define", "2"},
            {"What is default int value?", "0", "null", "undefined", "-1", "1"},
            {"Which of these is IDE?", "Eclipse", "JDK", "JRE", "JVM", "1"},
            {"Which one is used to print?", "System.out.println", "echo", "print", "System.write", "1"},
            {"Which one is not primitive?", "int", "boolean", "String", "char", "3"},
            {"Which function stops JVM?", "System.exit()", "close()", "quit()", "stop()", "1"},
            {"Which class handles date?", "Date", "Time", "Clock", "Calendar", "1"},
            {"Which is for multiple inheritance?", "interface", "class", "extends", "None", "1"},
            {"Which is Java's father?", "James Gosling", "Dennis Ritchie", "Guido", "Bjarne", "1"},
            {"Which keyword is for package?", "package", "import", "export", "include", "1"},
            {"Which stream reads file?", "FileReader", "FileWriter", "Output", "Scanner", "1"},
            {"What is the range of byte?", "-128 to 127", "-255 to 255", "0 to 255", "-127 to 127", "1"},
            {"What does 'super' refer to?", "Parent class", "Child class", "Same class", "None", "1"},
            {"What is lambda in Java?", "Anonymous function", "Interface", "Class", "Constructor", "1"},
            {"Which tool compiles Java?", "javac", "java", "jvm", "jre", "1"},
            {"Which is faster?", "Array", "ArrayList", "LinkedList", "Vector", "1"},
            {"Which stores key-value pairs?", "Map", "List", "Set", "Queue", "1"},
            {"Which collection maintains order?", "List", "Set", "Map", "Queue", "1"},
            {"Which statement is used for decision?", "if", "for", "while", "switch", "1"}
        }));
    }
}
