package studymate.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import studymate.agent.LearningAgent;
import studymate.agent.OpenAIAdapter;
import studymate.agent.RetrievalTool;
import studymate.agent.ToolRegistry;
import studymate.controller.AgentController;
import studymate.extraction.TextExtractor;
import studymate.model.AvailabilitySlot;
import studymate.model.Course;
import studymate.model.Flashcard;
import studymate.model.FlashcardDeck;
import studymate.model.LearningArtifact;
import studymate.model.Material;
import studymate.model.ProgressSummary;
import studymate.model.Question;
import studymate.model.QuestionResult;
import studymate.model.Quiz;
import studymate.model.QuizAttempt;
import studymate.model.StudyPlan;
import studymate.model.StudySession;
import studymate.model.TopicProgress;
import studymate.planning.DeadlineStrategy;
import studymate.planning.SchedulingStrategy;
import studymate.planning.WeakTopicStrategy;
import studymate.progress.ProgressTracker;
import studymate.repository.StudyRepository;
import studymate.service.MaterialService;
import studymate.service.QuizService;

public class StudyGUI extends JFrame {

    private static final long serialVersionUID = 1L;

    // =====================================================
    // BACKEND
    // =====================================================

    private final StudyRepository repository;
    private final Course course;

    private final MaterialService materialService;

    private final LearningAgent learningAgent;

    private final AgentController controller;

    private final ProgressTracker progressTracker;

    private final QuizService quizService;

    // =====================================================
    // GUI STATE
    // =====================================================

    private JLabel statusLabel;

    // F06
    private FlashcardDeck currentFlashcardDeck;
    private boolean flashcardAnswersVisible = false;

    // F07 / F08
    private Quiz currentQuiz;
    private UUID currentQuizAttemptId;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public StudyGUI() {

        // -------------------------------------------------
        // DATABASE + COURSE
        // -------------------------------------------------

        repository =
                new StudyRepository();

        course =
                repository.getOrCreateCourse(
                        "EECS3311 Software Design"
                );

        // -------------------------------------------------
        // F01 MATERIALS
        // -------------------------------------------------

        TextExtractor extractor =
                new TextExtractor();

        materialService =
                new MaterialService(
                        repository,
                        extractor
                );

        // -------------------------------------------------
        // AI BACKEND
        // -------------------------------------------------

        ToolRegistry toolRegistry =
                new ToolRegistry();

        toolRegistry.register(
                "retrieve",
                new RetrievalTool(
                        repository
                )
        );

        OpenAIAdapter openAI =
                new OpenAIAdapter();

        learningAgent =
                new LearningAgent(
                        openAI,
                        toolRegistry,
                        repository
                );

        // -------------------------------------------------
        // F08 + F09
        // -------------------------------------------------

        progressTracker =
                new ProgressTracker(
                        repository
                );

        quizService =
                new QuizService(
                        repository,
                        progressTracker
                );

        // Observer pattern:
        // after a new quiz attempt commits,
        // progress notifies the GUI.
        progressTracker.addListener(
                summary ->
                        SwingUtilities.invokeLater(
                                () -> {

                                    if (statusLabel != null) {

                                        statusLabel.setText(
                                                "Progress updated after committed quiz attempt."
                                        );
                                    }
                                }
                        )
        );

        // -------------------------------------------------
        // F10 CONTROLLER
        // -------------------------------------------------

        controller =
                new AgentController(
                        learningAgent,
                        repository
                );

        // -------------------------------------------------
        // WINDOW
        // -------------------------------------------------

        setTitle(
                "StudyMate - EECS3311 Software Design"
        );

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setMinimumSize(
                new Dimension(
                        1000,
                        700
                )
        );

        setSize(
                1100,
                750
        );

        setLocationRelativeTo(
                null
        );

        buildInterface();
    }

    // =====================================================
    // INTERFACE
    // =====================================================

    private void buildInterface() {

        setLayout(
                new BorderLayout(
                        10,
                        10
                )
        );

        add(
                createHeader(),
                BorderLayout.NORTH
        );

        add(
                createTabs(),
                BorderLayout.CENTER
        );

        add(
                createStatusBar(),
                BorderLayout.SOUTH
        );
    }

    // =====================================================
    // HEADER
    // =====================================================

    private JPanel createHeader() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        15,
                        20,
                        5,
                        20
                )
        );

        JLabel title =
                new JLabel(
                        "StudyMate"
                );

        title.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        28
                )
        );

        JLabel courseLabel =
                new JLabel(
                        course.getName(),
                        SwingConstants.RIGHT
                );

        courseLabel.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.PLAIN,
                        16
                )
        );

        panel.add(
                title,
                BorderLayout.WEST
        );

        panel.add(
                courseLabel,
                BorderLayout.EAST
        );

        return panel;
    }

    // =====================================================
    // TABS
    // =====================================================

    private JTabbedPane createTabs() {

        JTabbedPane tabs =
                new JTabbedPane();

        tabs.addTab(
                "Materials",
                createMaterialsPanel()
        );

        tabs.addTab(
                "AI Study",
                createAIStudyPanel()
        );

        tabs.addTab(
                "Study Plan",
                createPlanPanel()
        );

        tabs.addTab(
                "Flashcards",
                createFlashcardsPanel()
        );

        tabs.addTab(
                "Quiz",
                createQuizPanel()
        );

        tabs.addTab(
                "Progress",
                createProgressPanel()
        );

        return tabs;
    }

    // =====================================================
    // F01 - MATERIALS
    // =====================================================

    private JPanel createMaterialsPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Course Materials",
                        "Import and view PDF/TXT course materials."
                );

        JButton importButton =
                new JButton(
                        "Import Material"
                );

        JButton viewButton =
                new JButton(
                        "View Saved Materials"
                );

        importButton.addActionListener(
                e -> importMaterial(
                        feature.outputArea
                )
        );

        viewButton.addActionListener(
                e -> showSavedMaterials(
                        feature.outputArea
                )
        );

        addButtons(
                feature,
                importButton,
                viewButton
        );

        return feature.panel;
    }

    private void importMaterial(
            JTextArea outputArea) {

        JFileChooser chooser =
                new JFileChooser();

        chooser.setDialogTitle(
                "Select Course Material"
        );

        chooser.setFileSelectionMode(
                JFileChooser.FILES_ONLY
        );

        chooser.setFileFilter(
                new FileNameExtensionFilter(
                        "PDF and Text Files (*.pdf, *.txt)",
                        "pdf",
                        "txt"
                )
        );

        chooser.setAcceptAllFileFilterUsed(
                false
        );

        int choice =
                chooser.showOpenDialog(
                        this
                );

        if (choice
                != JFileChooser.APPROVE_OPTION) {

            statusLabel.setText(
                    "Import cancelled"
            );

            return;
        }

        File selectedFile =
                chooser.getSelectedFile();

        if (selectedFile == null) {

            return;
        }

        outputArea.setText(
                "Importing material...\n\n"
                        + selectedFile.getAbsolutePath()
        );

        statusLabel.setText(
                "Importing material..."
        );

        try {

            Material material =
                    materialService
                            .importMaterial(
                                    course.id(),
                                    selectedFile
                                            .getAbsolutePath()
                            );

            StringBuilder output =
                    new StringBuilder();

            output.append(
                    "IMPORT SUCCESSFUL\n"
            );

            output.append(
                    "==============================\n\n"
            );

            output.append(
                    "Material: "
                            + material.getTitle()
                            + "\n"
            );

            output.append(
                    "Chunks: "
                            + material
                                    .chunks()
                                    .size()
                            + "\n"
            );

            output.append(
                    "Material ID: "
                            + material.getId()
                            + "\n\n"
            );

            output.append(
                    "The material was saved "
                            + "to the StudyMate database."
            );

            outputArea.setText(
                    output.toString()
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Material imported successfully"
            );

            JOptionPane.showMessageDialog(
                    this,
                    "Material imported successfully.",
                    "StudyMate",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception ex) {

            outputArea.setText(
                    "IMPORT FAILED\n"
                            + "==============================\n\n"
                            + safeMessage(ex)
            );

            statusLabel.setText(
                    "Import failed"
            );

            JOptionPane.showMessageDialog(
                    this,
                    safeMessage(ex),
                    "Import Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showSavedMaterials(
            JTextArea outputArea) {

        try {

            List<Material> materials =
                    repository
                            .getMaterialsByCourse(
                                    course.id()
                            );

            StringBuilder output =
                    new StringBuilder();

            output.append(
                    "SAVED COURSE MATERIALS\n"
            );

            output.append(
                    "==============================\n\n"
            );

            output.append(
                    "Course: "
                            + course.getName()
                            + "\n\n"
            );

            output.append(
                    "Total materials: "
                            + materials.size()
                            + "\n\n"
            );

            if (materials.isEmpty()) {

                output.append(
                        "No materials have been imported yet."
                );

            } else {

                int number =
                        1;

                for (Material material :
                        materials) {

                    output.append(
                            number
                                    + ". "
                                    + material.getTitle()
                                    + "\n"
                    );

                    output.append(
                            "   ID: "
                                    + material.getId()
                                    + "\n"
                    );

                    output.append(
                            "   Chunks: "
                                    + material
                                            .chunks()
                                            .size()
                                    + "\n\n"
                    );

                    number++;
                }
            }

            outputArea.setText(
                    output.toString()
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Loaded "
                            + materials.size()
                            + " saved material(s)"
            );

        } catch (Exception ex) {

            outputArea.setText(
                    "Unable to load materials.\n\n"
                            + safeMessage(ex)
            );

            statusLabel.setText(
                    "Unable to load materials"
            );
        }
    }

    // =====================================================
    // F02 / F03 / F04 - AI STUDY
    // =====================================================

    private JPanel createAIStudyPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "AI Study Tools",
                        "Generate grounded summaries, concepts and course answers."
                );

        JButton summaryButton =
                new JButton(
                        "Summarise Materials"
                );

        JButton conceptsButton =
                new JButton(
                        "Extract Key Concepts"
                );

        JButton questionButton =
                new JButton(
                        "Ask a Question"
                );

        summaryButton.addActionListener(
                e -> runArtifactTask(
                        summaryButton,
                        feature.outputArea,
                        "Generating grounded summary...",
                        "AI SUMMARY",
                        () ->
                                learningAgent.summarise(
                                        course.id()
                                )
                )
        );

        conceptsButton.addActionListener(
                e -> runArtifactTask(
                        conceptsButton,
                        feature.outputArea,
                        "Extracting grounded key concepts...",
                        "KEY CONCEPTS",
                        () ->
                                learningAgent
                                        .extractConcepts(
                                                course.id()
                                        )
                )
        );

        questionButton.addActionListener(
                e -> askCourseQuestion(
                        questionButton,
                        feature.outputArea
                )
        );

        addButtons(
                feature,
                summaryButton,
                conceptsButton,
                questionButton
        );

        return feature.panel;
    }

    private void askCourseQuestion(
            JButton button,
            JTextArea outputArea) {

        String question =
                JOptionPane.showInputDialog(
                        this,
                        "Enter your question about "
                                + "the course material:",
                        "Ask StudyMate",
                        JOptionPane.QUESTION_MESSAGE
                );

        if (question == null) {

            statusLabel.setText(
                    "Question cancelled"
            );

            return;
        }

        question =
                question.trim();

        if (question.isBlank()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a question.",
                    "StudyMate",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        final String submittedQuestion =
                question;

        runArtifactTask(
                button,
                outputArea,
                "Searching course evidence "
                        + "and generating an answer...",
                "COURSE QUESTION & ANSWER",
                () ->
                        learningAgent.answerQuestion(
                                course.id(),
                                submittedQuestion
                        ),
                "Question: "
                        + submittedQuestion
                        + "\n\n"
        );
    }

    private void runArtifactTask(
            JButton button,
            JTextArea outputArea,
            String runningMessage,
            String heading,
            Callable<LearningArtifact> task) {

        runArtifactTask(
                button,
                outputArea,
                runningMessage,
                heading,
                task,
                ""
        );
    }

    private void runArtifactTask(
            JButton button,
            JTextArea outputArea,
            String runningMessage,
            String heading,
            Callable<LearningArtifact> task,
            String prefix) {

        button.setEnabled(
                false
        );

        outputArea.setText(
                runningMessage
        );

        statusLabel.setText(
                runningMessage
        );

        SwingWorker<LearningArtifact, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected LearningArtifact doInBackground()
                            throws Exception {

                        return task.call();
                    }

                    @Override
                    protected void done() {

                        button.setEnabled(
                                true
                        );

                        try {

                            LearningArtifact artifact =
                                    get();

                            StringBuilder output =
                                    new StringBuilder();

                            output.append(
                                    heading
                            );

                            output.append(
                                    "\n==============================\n\n"
                            );

                            if (prefix != null
                                    && !prefix.isBlank()) {

                                output.append(
                                        prefix
                                );
                            }

                            output.append(
                                    artifact.content()
                            );

                            output.append(
                                    "\n\n==============================\n"
                            );

                            List<UUID> sourceIds =
                                    artifact.getSourceIds();

                            output.append(
                                    "Sources used: "
                                            + sourceIds.size()
                                            + "\n"
                            );

                            if (!sourceIds.isEmpty()) {

                                output.append(
                                        "\nSource IDs:\n"
                                );

                                int number =
                                        1;

                                for (UUID sourceId :
                                        sourceIds) {

                                    output.append(
                                            number
                                                    + ". "
                                                    + sourceId
                                                    + "\n"
                                    );

                                    number++;
                                }
                            }

                            outputArea.setText(
                                    output.toString()
                            );

                            outputArea.setCaretPosition(
                                    0
                            );

                            statusLabel.setText(
                                    heading
                                            + " generated successfully"
                            );

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            showAITaskError(
                                    outputArea,
                                    "Operation interrupted."
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            showAITaskError(
                                    outputArea,
                                    cause == null
                                            ? safeMessage(ex)
                                            : safeMessage(cause)
                            );
                        }
                    }
                };

        worker.execute();
    }

    // =====================================================
    // F05 + F10 - STUDY PLAN
    // =====================================================

    private JPanel createPlanPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Study Planning",
                        "Generate, preview, confirm and undo study-plan revisions."
                );

        JButton generateButton =
                new JButton(
                        "Generate Study Plan"
                );

        JButton latestButton =
                new JButton(
                        "Show Latest Plan"
                );

        JButton adaptButton =
                new JButton(
                        "Adapt Plan"
                );

        JButton confirmButton =
                new JButton(
                        "Confirm Revision"
                );

        JButton rejectButton =
                new JButton(
                        "Reject Revision"
                );

        JButton undoButton =
                new JButton(
                        "Undo Revision"
                );

        generateButton.addActionListener(
                e -> generateStudyPlanGUI(
                        generateButton,
                        feature.outputArea
                )
        );

        latestButton.addActionListener(
                e -> showLatestPlan(
                        feature.outputArea
                )
        );

        adaptButton.addActionListener(
                e -> previewPlanRevision(
                        adaptButton,
                        feature.outputArea
                )
        );

        confirmButton.addActionListener(
                e -> confirmPlanRevision(
                        feature.outputArea
                )
        );

        rejectButton.addActionListener(
                e -> rejectPlanRevision(
                        feature.outputArea
                )
        );

        undoButton.addActionListener(
                e -> undoPlanRevision(
                        feature.outputArea
                )
        );

        addButtons(
                feature,
                generateButton,
                latestButton,
                adaptButton,
                confirmButton,
                rejectButton,
                undoButton
        );

        return feature.panel;
    }

    private void generateStudyPlanGUI(
            JButton button,
            JTextArea outputArea) {

        PlanInput input;

        try {

            input =
                    requestPlanInput(
                            "Generate Study Plan"
                    );

        } catch (IllegalArgumentException ex) {

            showPlanInputError(
                    ex.getMessage()
            );

            return;
        }

        if (input == null) {

            statusLabel.setText(
                    "Study-plan generation cancelled"
            );

            return;
        }

        button.setEnabled(
                false
        );

        outputArea.setText(
                "Generating grounded topic estimates "
                        + "and scheduling sessions..."
        );

        statusLabel.setText(
                "Generating study plan..."
        );

        SwingWorker<StudyPlan, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected StudyPlan doInBackground()
                            throws Exception {

                        return learningAgent
                                .generateStudyPlan(
                                        course.id(),
                                        input.examDate,
                                        input.availability,
                                        input.strategy
                                );
                    }

                    @Override
                    protected void done() {

                        button.setEnabled(
                                true
                        );

                        try {

                            StudyPlan plan =
                                    get();

                            StringBuilder output =
                                    new StringBuilder();

                            output.append(
                                    "GENERATED STUDY PLAN\n"
                            );

                            output.append(
                                    "==============================\n"
                            );

                            output.append(
                                    formatStudyPlan(
                                            plan
                                    )
                            );

                            output.append(
                                    "\n"
                            );

                            if (plan.isFeasible()) {

                                output.append(
                                        "The feasible plan was saved."
                                );

                                statusLabel.setText(
                                        "Study plan generated and saved"
                                );

                            } else {

                                output.append(
                                        "The plan is not feasible "
                                                + "and was NOT saved."
                                );

                                statusLabel.setText(
                                        "Study plan has insufficient capacity"
                                );
                            }

                            outputArea.setText(
                                    output.toString()
                            );

                            outputArea.setCaretPosition(
                                    0
                            );

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            showPlanError(
                                    outputArea,
                                    "Study-plan generation interrupted."
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            showPlanError(
                                    outputArea,
                                    cause == null
                                            ? safeMessage(ex)
                                            : safeMessage(cause)
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void showLatestPlan(
            JTextArea outputArea) {

        try {

            StudyPlan plan =
                    repository
                            .findLatestPlanByCourse(
                                    course.id()
                            );

            if (plan == null) {

                outputArea.setText(
                        "LATEST STUDY PLAN\n"
                                + "==============================\n\n"
                                + "No saved study plan exists."
                );

                statusLabel.setText(
                        "No saved study plan"
                );

                return;
            }

            outputArea.setText(
                    "LATEST STUDY PLAN\n"
                            + "==============================\n"
                            + formatStudyPlan(plan)
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Latest study plan loaded"
            );

        } catch (Exception ex) {

            showPlanError(
                    outputArea,
                    safeMessage(ex)
            );
        }
    }

    private void previewPlanRevision(
            JButton button,
            JTextArea outputArea) {

        StudyPlan current;

        try {

            current =
                    repository
                            .findLatestPlanByCourse(
                                    course.id()
                            );

        } catch (Exception ex) {

            showPlanError(
                    outputArea,
                    safeMessage(ex)
            );

            return;
        }

        if (current == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Generate a study plan first.",
                    "No Study Plan",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        PlanInput input;

        try {

            input =
                    requestPlanInput(
                            "Adapt Study Plan"
                    );

        } catch (IllegalArgumentException ex) {

            showPlanInputError(
                    ex.getMessage()
            );

            return;
        }

        if (input == null) {

            statusLabel.setText(
                    "Plan adaptation cancelled"
            );

            return;
        }

        button.setEnabled(
                false
        );

        outputArea.setText(
                "Generating revision preview...\n\n"
                        + "The saved plan is not being changed."
        );

        statusLabel.setText(
                "Generating revision preview..."
        );

        SwingWorker<StudyPlan, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected StudyPlan doInBackground()
                            throws Exception {

                        return controller
                                .previewRevision(
                                        course.id(),
                                        input.examDate,
                                        input.availability,
                                        input.strategy
                                );
                    }

                    @Override
                    protected void done() {

                        button.setEnabled(
                                true
                        );

                        try {

                            StudyPlan preview =
                                    get();

                            StringBuilder output =
                                    new StringBuilder();

                            output.append(
                                    "REVISION PREVIEW\n"
                            );

                            output.append(
                                    "==============================\n"
                            );

                            output.append(
                                    formatStudyPlan(
                                            preview
                                    )
                            );

                            output.append(
                                    "\n"
                            );

                            if (preview.isFeasible()) {

                                output.append(
                                        "PREVIEW ONLY.\n"
                                );

                                output.append(
                                        "The saved plan has NOT changed.\n\n"
                                );

                                output.append(
                                        "Use \"Confirm Revision\" "
                                                + "to save this preview,\n"
                                );

                                output.append(
                                        "or \"Reject Revision\" "
                                                + "to discard it."
                                );

                                statusLabel.setText(
                                        "Revision preview ready"
                                );

                            } else {

                                output.append(
                                        "This revision is not feasible.\n"
                                );

                                output.append(
                                        "Nothing was saved."
                                );

                                statusLabel.setText(
                                        "Revision has insufficient capacity"
                                );
                            }

                            outputArea.setText(
                                    output.toString()
                            );

                            outputArea.setCaretPosition(
                                    0
                            );

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            showPlanError(
                                    outputArea,
                                    "Plan adaptation interrupted."
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            showPlanError(
                                    outputArea,
                                    cause == null
                                            ? safeMessage(ex)
                                            : safeMessage(cause)
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void confirmPlanRevision(
            JTextArea outputArea) {

        if (!controller.hasPendingRevision()) {

            JOptionPane.showMessageDialog(
                    this,
                    "There is no pending revision to confirm.",
                    "StudyMate",
                    JOptionPane.WARNING_MESSAGE
            );

            statusLabel.setText(
                    "No pending revision"
            );

            return;
        }

        int confirmation =
                JOptionPane.showConfirmDialog(
                        this,
                        "Save the pending study-plan revision?",
                        "Confirm Revision",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (confirmation
                != JOptionPane.YES_OPTION) {

            statusLabel.setText(
                    "Confirmation cancelled"
            );

            return;
        }

        try {

            StudyPlan saved =
                    controller.confirmRevision();

            outputArea.setText(
                    "REVISION CONFIRMED\n"
                            + "==============================\n"
                            + formatStudyPlan(saved)
                            + "\n"
                            + "The revision is now saved.\n"
                            + "One-level undo is available."
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Plan revision confirmed"
            );

        } catch (Exception ex) {

            showPlanError(
                    outputArea,
                    safeMessage(ex)
            );
        }
    }

    private void rejectPlanRevision(
            JTextArea outputArea) {

        if (!controller.hasPendingRevision()) {

            JOptionPane.showMessageDialog(
                    this,
                    "There is no pending revision to reject.",
                    "StudyMate",
                    JOptionPane.WARNING_MESSAGE
            );

            statusLabel.setText(
                    "No pending revision"
            );

            return;
        }

        controller.rejectRevision();

        outputArea.setText(
                "REVISION REJECTED\n"
                        + "==============================\n\n"
                        + "The pending revision was discarded.\n"
                        + "The saved study plan was not changed."
        );

        statusLabel.setText(
                "Plan revision rejected"
        );
    }

    private void undoPlanRevision(
            JTextArea outputArea) {

        if (!controller.canUndo()) {

            JOptionPane.showMessageDialog(
                    this,
                    "There is no confirmed revision "
                            + "available to undo.",
                    "StudyMate",
                    JOptionPane.WARNING_MESSAGE
            );

            statusLabel.setText(
                    "No revision available to undo"
            );

            return;
        }

        int confirmation =
                JOptionPane.showConfirmDialog(
                        this,
                        "Undo the last confirmed revision?",
                        "Undo Revision",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (confirmation
                != JOptionPane.YES_OPTION) {

            statusLabel.setText(
                    "Undo cancelled"
            );

            return;
        }

        try {

            StudyPlan restored =
                    controller.undoLastRevision();

            outputArea.setText(
                    "REVISION UNDONE\n"
                            + "==============================\n"
                            + formatStudyPlan(restored)
                            + "\n"
                            + "The previous schedule was restored "
                            + "as a new plan version."
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Last plan revision undone"
            );

        } catch (Exception ex) {

            showPlanError(
                    outputArea,
                    safeMessage(ex)
            );
        }
    }

    // =====================================================
    // PLAN INPUT
    // =====================================================

    private PlanInput requestPlanInput(
            String title) {

        String examText =
                JOptionPane.showInputDialog(
                        this,
                        "Exam date (YYYY-MM-DD):",
                        title,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (examText == null) {

            return null;
        }

        LocalDate examDate;

        try {

            examDate =
                    LocalDate.parse(
                            examText.trim()
                    );

        } catch (DateTimeParseException ex) {

            throw new IllegalArgumentException(
                    "Exam date must use YYYY-MM-DD."
            );
        }

        if (!examDate.isAfter(
                LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Exam date must be in the future."
            );
        }

        String dayCountText =
                JOptionPane.showInputDialog(
                        this,
                        "Number of available study days:",
                        title,
                        JOptionPane.QUESTION_MESSAGE
                );

        if (dayCountText == null) {

            return null;
        }

        int dayCount;

        try {

            dayCount =
                    Integer.parseInt(
                            dayCountText.trim()
                    );

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Number of study days must be a number."
            );
        }

        if (dayCount <= 0) {

            throw new IllegalArgumentException(
                    "At least one available study day is required."
            );
        }

        List<AvailabilitySlot> availability =
                new ArrayList<>();

        for (int i = 1;
             i <= dayCount;
             i++) {

            String dateText =
                    JOptionPane.showInputDialog(
                            this,
                            "Availability "
                                    + i
                                    + " date (YYYY-MM-DD):",
                            title,
                            JOptionPane.QUESTION_MESSAGE
                    );

            if (dateText == null) {

                return null;
            }

            LocalDate date;

            try {

                date =
                        LocalDate.parse(
                                dateText.trim()
                        );

            } catch (DateTimeParseException ex) {

                throw new IllegalArgumentException(
                        "Availability date must use YYYY-MM-DD."
                );
            }

            if (date.isBefore(
                    LocalDate.now())) {

                throw new IllegalArgumentException(
                        "Availability cannot be in the past."
                );
            }

            if (!date.isBefore(
                    examDate)) {

                throw new IllegalArgumentException(
                        "Availability must be before the exam date."
                );
            }

            String minutesText =
                    JOptionPane.showInputDialog(
                            this,
                            "Available minutes on "
                                    + date
                                    + ":",
                            title,
                            JOptionPane.QUESTION_MESSAGE
                    );

            if (minutesText == null) {

                return null;
            }

            int minutes;

            try {

                minutes =
                        Integer.parseInt(
                                minutesText.trim()
                        );

            } catch (NumberFormatException ex) {

                throw new IllegalArgumentException(
                        "Available minutes must be a number."
                );
            }

            if (minutes <= 0) {

                throw new IllegalArgumentException(
                        "Available minutes must be positive."
                );
            }

            availability.add(
                    new AvailabilitySlot(
                            date,
                            minutes
                    )
            );
        }

        Object strategyChoice =
                JOptionPane.showInputDialog(
                        this,
                        "Choose scheduling strategy:",
                        title,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{
                                "Deadline",
                                "Weak Topic"
                        },
                        "Weak Topic"
                );

        if (strategyChoice == null) {

            return null;
        }

        SchedulingStrategy strategy;

        if ("Deadline".equals(
                strategyChoice.toString())) {

            strategy =
                    new DeadlineStrategy();

        } else {

            strategy =
                    new WeakTopicStrategy();
        }

        return new PlanInput(
                examDate,
                availability,
                strategy
        );
    }

    // =====================================================
    // PLAN DISPLAY
    // =====================================================

    private String formatStudyPlan(
            StudyPlan plan) {

        StringBuilder output =
                new StringBuilder();

        output.append(
                "\n"
        );

        output.append(
                "Plan ID: "
                        + plan.getId()
                        + "\n"
        );

        output.append(
                "Version: "
                        + plan.getVersion()
                        + "\n"
        );

        output.append(
                "Exam date: "
                        + plan.getExamDate()
                        + "\n"
        );

        output.append(
                "Strategy: "
                        + plan.getStrategyName()
                        + "\n\n"
        );

        if (plan.getSessions()
                .isEmpty()) {

            output.append(
                    "No sessions could be scheduled.\n"
            );

        } else {

            output.append(
                    "Scheduled sessions:\n\n"
            );

            for (StudySession session :
                    plan.getSessions()) {

                output.append(
                        "- "
                                + session.getDate()
                                + " | "
                                + session.getTopic()
                                + " | "
                                + session.getMinutes()
                                + " min\n"
                );
            }
        }

        output.append(
                "\n"
        );

        if (plan.isFeasible()) {

            output.append(
                    "Plan status: FEASIBLE\n"
            );

        } else {

            output.append(
                    "Plan status: INSUFFICIENT CAPACITY\n"
            );

            if (!plan.getUnscheduledTopics()
                    .isEmpty()) {

                output.append(
                        "\nUnscheduled work:\n"
                );

                for (String topic :
                        plan.getUnscheduledTopics()) {

                    output.append(
                            "- "
                                    + topic
                                    + "\n"
                    );
                }
            }
        }

        return output.toString();
    }

    private void showPlanInputError(
            String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Invalid Study Plan Input",
                JOptionPane.WARNING_MESSAGE
        );

        statusLabel.setText(
                "Invalid study-plan input"
        );
    }

    private void showPlanError(
            JTextArea outputArea,
            String message) {

        outputArea.setText(
                "STUDY PLAN OPERATION FAILED\n"
                        + "==============================\n\n"
                        + message
        );

        statusLabel.setText(
                "Study-plan operation failed"
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                "Study Plan Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =====================================================
    // F06 - FLASHCARDS
    // =====================================================

    private JPanel createFlashcardsPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Flashcards",
                        "Generate grounded flashcards and reveal answers for practice."
                );

        JButton generateButton =
                new JButton(
                        "Generate Flashcards"
                );

        JButton revealButton =
                new JButton(
                        "Reveal Answers"
                );

        revealButton.setEnabled(
                false
        );

        generateButton.addActionListener(
                e -> generateFlashcardsGUI(
                        generateButton,
                        revealButton,
                        feature.outputArea
                )
        );

        revealButton.addActionListener(
                e -> {

                    if (currentFlashcardDeck == null
                            || currentFlashcardDeck
                                    .getCards()
                                    .isEmpty()) {

                        return;
                    }

                    flashcardAnswersVisible =
                            !flashcardAnswersVisible;

                    if (flashcardAnswersVisible) {

                        revealButton.setText(
                                "Hide Answers"
                        );

                    } else {

                        revealButton.setText(
                                "Reveal Answers"
                        );
                    }

                    displayFlashcardDeck(
                            feature.outputArea,
                            currentFlashcardDeck
                    );
                }
        );

        addButtons(
                feature,
                generateButton,
                revealButton
        );

        return feature.panel;
    }

    private void generateFlashcardsGUI(
            JButton generateButton,
            JButton revealButton,
            JTextArea outputArea) {

        String topic =
                JOptionPane.showInputDialog(
                        this,
                        "Enter a flashcard topic:",
                        "Generate Flashcards",
                        JOptionPane.QUESTION_MESSAGE
                );

        if (topic == null) {

            statusLabel.setText(
                    "Flashcard generation cancelled"
            );

            return;
        }

        topic =
                topic.trim();

        if (topic.isBlank()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a topic.",
                    "Invalid Topic",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        String countText =
                JOptionPane.showInputDialog(
                        this,
                        "How many flashcards? (1-30)",
                        "Generate Flashcards",
                        JOptionPane.QUESTION_MESSAGE
                );

        if (countText == null) {

            statusLabel.setText(
                    "Flashcard generation cancelled"
            );

            return;
        }

        int count;

        try {

            count =
                    Integer.parseInt(
                            countText.trim()
                    );

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Flashcard count must be a number.",
                    "Invalid Count",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        if (count < 1
                || count > 30) {

            JOptionPane.showMessageDialog(
                    this,
                    "Flashcard count must be between 1 and 30.",
                    "Invalid Count",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        final String submittedTopic =
                topic;

        final int submittedCount =
                count;

        generateButton.setEnabled(
                false
        );

        revealButton.setEnabled(
                false
        );

        currentFlashcardDeck =
                null;

        flashcardAnswersVisible =
                false;

        revealButton.setText(
                "Reveal Answers"
        );

        outputArea.setText(
                "Generating grounded flashcards...\n\n"
                        + "Topic: "
                        + submittedTopic
                        + "\n"
                        + "Requested cards: "
                        + submittedCount
        );

        statusLabel.setText(
                "Generating flashcards..."
        );

        SwingWorker<FlashcardDeck, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected FlashcardDeck doInBackground()
                            throws Exception {

                        return learningAgent
                                .generateFlashcards(
                                        course.id(),
                                        submittedTopic,
                                        submittedCount
                                );
                    }

                    @Override
                    protected void done() {

                        generateButton.setEnabled(
                                true
                        );

                        try {

                            FlashcardDeck deck =
                                    get();

                            currentFlashcardDeck =
                                    deck;

                            flashcardAnswersVisible =
                                    false;

                            if (deck.getCards()
                                    .isEmpty()) {

                                revealButton.setEnabled(
                                        false
                                );

                                outputArea.setText(
                                        "FLASHCARDS\n"
                                                + "==============================\n\n"
                                                + "Topic: "
                                                + deck.getTopic()
                                                + "\n"
                                                + "Requested: "
                                                + deck.getRequestedCount()
                                                + "\n"
                                                + "Generated: 0\n\n"
                                                + "No supported cards were generated.\n"
                                                + "No incomplete deck was saved."
                                );

                                statusLabel.setText(
                                        "No supported flashcards generated"
                                );

                                return;
                            }

                            revealButton.setEnabled(
                                    true
                            );

                            revealButton.setText(
                                    "Reveal Answers"
                            );

                            displayFlashcardDeck(
                                    outputArea,
                                    deck
                            );

                            statusLabel.setText(
                                    "Generated "
                                            + deck.getCards().size()
                                            + " grounded flashcard(s)"
                            );

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            showFlashcardError(
                                    outputArea,
                                    safeMessage(ex)
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            showFlashcardError(
                                    outputArea,
                                    cause == null
                                            ? safeMessage(ex)
                                            : safeMessage(cause)
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void displayFlashcardDeck(
            JTextArea outputArea,
            FlashcardDeck deck) {

        StringBuilder output =
                new StringBuilder();

        output.append(
                "FLASHCARD DECK\n"
        );

        output.append(
                "==============================\n\n"
        );

        output.append(
                "Topic: "
                        + deck.getTopic()
                        + "\n"
        );

        output.append(
                "Requested: "
                        + deck.getRequestedCount()
                        + "\n"
        );

        output.append(
                "Generated: "
                        + deck.getCards().size()
                        + "\n"
        );

        if (deck.isReducedDeck()) {

            output.append(
                    "Note: StudyMate generated a smaller "
                            + "supported deck.\n"
            );
        }

        output.append(
                "\n"
        );

        int number =
                1;

        for (Flashcard card :
                deck.getCards()) {

            output.append(
                    "CARD "
                            + number
                            + "\n"
            );

            output.append(
                    "------------------------------\n"
            );

            output.append(
                    "Q: "
                            + card.getQuestion()
                            + "\n"
            );

            if (flashcardAnswersVisible) {

                output.append(
                        "A: "
                                + card.getAnswer()
                                + "\n"
                );

            } else {

                output.append(
                        "A: [hidden - click Reveal Answers]\n"
                );
            }

            output.append(
                    "Source: "
                            + card.getSourceId()
                            + "\n\n"
            );

            number++;
        }

        output.append(
                "==============================\n"
        );

        if (flashcardAnswersVisible) {

            output.append(
                    "Answers are currently visible."
            );

        } else {

            output.append(
                    "Use \"Reveal Answers\" to check your answers."
            );
        }

        outputArea.setText(
                output.toString()
        );

        outputArea.setCaretPosition(
                0
        );
    }

    private void showFlashcardError(
            JTextArea outputArea,
            String message) {

        currentFlashcardDeck =
                null;

        flashcardAnswersVisible =
                false;

        outputArea.setText(
                "FLASHCARD GENERATION FAILED\n"
                        + "==============================\n\n"
                        + message
        );

        statusLabel.setText(
                "Flashcard generation failed"
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                "Flashcard Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =====================================================
    // F07 + F08 - QUIZ
    // =====================================================

    private JPanel createQuizPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Quiz",
                        "Generate a grounded quiz, answer it, and grade it deterministically."
                );

        JButton generateButton =
                new JButton(
                        "Generate Quiz"
                );

        JButton gradeButton =
                new JButton(
                        "Grade Quiz"
                );

        gradeButton.setEnabled(
                false
        );

        generateButton.addActionListener(
                e -> generateQuizGUI(
                        generateButton,
                        gradeButton,
                        feature.outputArea
                )
        );

        gradeButton.addActionListener(
                e -> gradeCurrentQuiz(
                        generateButton,
                        gradeButton,
                        feature.outputArea
                )
        );

        addButtons(
                feature,
                generateButton,
                gradeButton
        );

        return feature.panel;
    }

    // =====================================================
    // F07 - GENERATE QUIZ
    // =====================================================

    private void generateQuizGUI(
            JButton generateButton,
            JButton gradeButton,
            JTextArea outputArea) {

        String topic =
                JOptionPane.showInputDialog(
                        this,
                        "Enter quiz topic:",
                        "Generate Quiz",
                        JOptionPane.QUESTION_MESSAGE
                );

        if (topic == null) {

            statusLabel.setText(
                    "Quiz generation cancelled"
            );

            return;
        }

        topic =
                topic.trim();

        if (topic.isBlank()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a quiz topic.",
                    "Invalid Topic",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        Object difficultyChoice =
                JOptionPane.showInputDialog(
                        this,
                        "Choose difficulty:",
                        "Generate Quiz",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new String[]{
                                "Easy",
                                "Medium",
                                "Hard"
                        },
                        "Medium"
                );

        if (difficultyChoice == null) {

            statusLabel.setText(
                    "Quiz generation cancelled"
            );

            return;
        }

        String countText =
                JOptionPane.showInputDialog(
                        this,
                        "How many questions? (1-20)",
                        "Generate Quiz",
                        JOptionPane.QUESTION_MESSAGE
                );

        if (countText == null) {

            statusLabel.setText(
                    "Quiz generation cancelled"
            );

            return;
        }

        int count;

        try {

            count =
                    Integer.parseInt(
                            countText.trim()
                    );

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Quiz count must be a number.",
                    "Invalid Count",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        if (count < 1
                || count > 20) {

            JOptionPane.showMessageDialog(
                    this,
                    "Quiz count must be between 1 and 20.",
                    "Invalid Count",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        final String submittedTopic =
                topic;

        final String submittedDifficulty =
                difficultyChoice
                        .toString();

        final int submittedCount =
                count;

        generateButton.setEnabled(
                false
        );

        gradeButton.setEnabled(
                false
        );

        currentQuiz =
                null;

        currentQuizAttemptId =
                null;

        outputArea.setText(
                "Generating grounded quiz...\n\n"
                        + "Topic: "
                        + submittedTopic
                        + "\n"
                        + "Difficulty: "
                        + submittedDifficulty
                        + "\n"
                        + "Questions: "
                        + submittedCount
        );

        statusLabel.setText(
                "Generating quiz..."
        );

        SwingWorker<Quiz, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Quiz doInBackground()
                            throws Exception {

                        return learningAgent
                                .generateQuiz(
                                        course.id(),
                                        submittedTopic,
                                        submittedDifficulty,
                                        submittedCount
                                );
                    }

                    @Override
                    protected void done() {

                        generateButton.setEnabled(
                                true
                        );

                        try {

                            Quiz quiz =
                                    get();

                            currentQuiz =
                                    quiz;

                            currentQuizAttemptId =
                                    UUID.randomUUID();

                            displayQuiz(
                                    outputArea,
                                    quiz
                            );

                            gradeButton.setEnabled(
                                    true
                            );

                            statusLabel.setText(
                                    "Quiz generated successfully"
                            );

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            showQuizError(
                                    outputArea,
                                    "Quiz generation interrupted."
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            showQuizError(
                                    outputArea,
                                    cause == null
                                            ? safeMessage(ex)
                                            : safeMessage(cause)
                            );
                        }
                    }
                };

        worker.execute();
    }

    private void displayQuiz(
            JTextArea outputArea,
            Quiz quiz) {

        StringBuilder output =
                new StringBuilder();

        output.append(
                "QUIZ\n"
        );

        output.append(
                "==============================\n\n"
        );

        output.append(
                "Quiz ID: "
                        + quiz.getId()
                        + "\n"
        );

        output.append(
                "Topic: "
                        + quiz.getTopic()
                        + "\n"
        );

        output.append(
                "Difficulty: "
                        + quiz.getDifficulty()
                        + "\n"
        );

        output.append(
                "Questions: "
                        + quiz.getQuestions().size()
                        + "\n\n"
        );

        int number =
                1;

        for (Question question :
                quiz.getQuestions()) {

            output.append(
                    number
                            + ". "
                            + question.getText()
                            + "\n"
            );

            List<String> options =
                    question.getOptions();

            output.append(
                    "   A. "
                            + options.get(0)
                            + "\n"
            );

            output.append(
                    "   B. "
                            + options.get(1)
                            + "\n"
            );

            output.append(
                    "   C. "
                            + options.get(2)
                            + "\n"
            );

            output.append(
                    "   D. "
                            + options.get(3)
                            + "\n"
            );

            output.append(
                    "   Source: "
                            + question.getSourceId()
                            + "\n\n"
            );

            number++;
        }

        output.append(
                "==============================\n"
        );

        output.append(
                "Answer keys are hidden.\n"
        );

        output.append(
                "Click \"Grade Quiz\" to submit your answers."
        );

        outputArea.setText(
                output.toString()
        );

        outputArea.setCaretPosition(
                0
        );
    }

    // =====================================================
    // F08 - GRADE QUIZ
    // =====================================================

    private void gradeCurrentQuiz(
            JButton generateButton,
            JButton gradeButton,
            JTextArea outputArea) {

        if (currentQuiz == null) {

            JOptionPane.showMessageDialog(
                    this,
                    "Generate a quiz first.",
                    "No Quiz",
                    JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        List<Integer> answers =
                collectQuizAnswers(
                        currentQuiz
                );

        if (answers == null) {

            statusLabel.setText(
                    "Quiz submission cancelled"
            );

            return;
        }

        int skipped =
                0;

        for (Integer answer :
                answers) {

            if (answer == null) {

                skipped++;
            }
        }

        if (skipped > 0) {

            int confirm =
                    JOptionPane.showConfirmDialog(
                            this,
                            "You skipped "
                                    + skipped
                                    + " question(s).\n\n"
                                    + "Skipped questions count as zero credit.\n"
                                    + "Submit anyway?",
                            "Confirm Skipped Answers",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );

            if (confirm
                    != JOptionPane.YES_OPTION) {

                statusLabel.setText(
                        "Quiz submission cancelled"
                );

                return;
            }
        }

        try {

            QuizAttempt attempt =
                    quizService.grade(
                            currentQuiz.getId(),
                            currentQuizAttemptId,
                            answers
                    );

            String deterministicOutput =
                    buildQuizResultOutput(
                            currentQuiz,
                            attempt
                    );

            outputArea.setText(
                    deterministicOutput
                            + "\n\n"
                            + "Generating optional AI feedback "
                            + "for incorrect answers..."
            );

            outputArea.setCaretPosition(
                    0
            );

            gradeButton.setEnabled(
                    false
            );

            if (attempt.getCorrectCount()
                    == attempt.getGradedCount()) {

                outputArea.setText(
                        deterministicOutput
                                + "\n\n"
                                + "MISTAKE FEEDBACK\n"
                                + "==============================\n\n"
                                + "Perfect score. No mistake feedback is needed."
                );

                statusLabel.setText(
                        "Quiz graded: "
                                + attempt.getCorrectCount()
                                + "/"
                                + attempt.getGradedCount()
                );

                return;
            }

            generateButton.setEnabled(
                    false
            );

            requestMistakeFeedback(
                    generateButton,
                    outputArea,
                    currentQuiz,
                    attempt,
                    deterministicOutput
            );

        } catch (Exception ex) {

            showQuizError(
                    outputArea,
                    safeMessage(ex)
            );
        }
    }

    private List<Integer> collectQuizAnswers(
            Quiz quiz) {

        List<Integer> answers =
                new ArrayList<>();

        List<Question> questions =
                quiz.getQuestions();

        for (int i = 0;
             i < questions.size();
             i++) {

            Question question =
                    questions.get(i);

            String prompt =
                    buildQuizAnswerPrompt(
                            i + 1,
                            question
                    );

            Object choice =
                    JOptionPane.showInputDialog(
                            this,
                            prompt,
                            "Quiz Question "
                                    + (i + 1)
                                    + " of "
                                    + questions.size(),
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new String[]{
                                    "A",
                                    "B",
                                    "C",
                                    "D",
                                    "Skip"
                            },
                            "A"
                    );

            if (choice == null) {

                return null;
            }

            String answer =
                    choice.toString();

            switch (answer) {

                case "A" ->
                        answers.add(0);

                case "B" ->
                        answers.add(1);

                case "C" ->
                        answers.add(2);

                case "D" ->
                        answers.add(3);

                case "Skip" ->
                        answers.add(null);

                default ->
                        throw new IllegalStateException(
                                "Unexpected quiz answer."
                        );
            }
        }

        return answers;
    }

    private String buildQuizAnswerPrompt(
            int number,
            Question question) {

        List<String> options =
                question.getOptions();

        return number
                + ". "
                + question.getText()
                + "\n\n"
                + "A. "
                + options.get(0)
                + "\n"
                + "B. "
                + options.get(1)
                + "\n"
                + "C. "
                + options.get(2)
                + "\n"
                + "D. "
                + options.get(3)
                + "\n\n"
                + "Choose your answer:";
    }

    private String buildQuizResultOutput(
            Quiz quiz,
            QuizAttempt attempt) {

        StringBuilder output =
                new StringBuilder();

        output.append(
                "QUIZ RESULT\n"
        );

        output.append(
                "==============================\n\n"
        );

        output.append(
                "Quiz ID: "
                        + quiz.getId()
                        + "\n"
        );

        output.append(
                "Attempt ID: "
                        + attempt.getId()
                        + "\n"
        );

        output.append(
                "Topic: "
                        + quiz.getTopic()
                        + "\n"
        );

        output.append(
                "Difficulty: "
                        + quiz.getDifficulty()
                        + "\n\n"
        );

        output.append(
                "Correct: "
                        + attempt.getCorrectCount()
                        + " / "
                        + attempt.getGradedCount()
                        + "\n"
        );

        output.append(
                "Score: "
                        + String.format(
                                "%.1f",
                                attempt.getScorePercent()
                        )
                        + "%\n\n"
        );

        output.append(
                "The score above was calculated "
                        + "deterministically from the stored answer key.\n"
        );

        output.append(
                "AI feedback cannot modify this score.\n\n"
        );

        List<Question> questions =
                quiz.getQuestions();

        List<QuestionResult> results =
                attempt.getResults();

        for (int i = 0;
             i < results.size();
             i++) {

            Question question =
                    questions.get(i);

            QuestionResult result =
                    results.get(i);

            output.append(
                    "QUESTION "
                            + (i + 1)
                            + "\n"
            );

            output.append(
                    "------------------------------\n"
            );

            output.append(
                    question.getText()
                            + "\n"
            );

            if (result.isSkipped()) {

                output.append(
                        "Result: SKIPPED / INCORRECT\n"
                );

                output.append(
                        "Selected: [skipped]\n"
                );

            } else if (result.isCorrect()) {

                output.append(
                        "Result: CORRECT\n"
                );

                output.append(
                        "Selected: "
                                + formatQuizOption(
                                        question,
                                        result.getSelectedOptionIndex()
                                )
                                + "\n"
                );

            } else {

                output.append(
                        "Result: INCORRECT\n"
                );

                output.append(
                        "Selected: "
                                + formatQuizOption(
                                        question,
                                        result.getSelectedOptionIndex()
                                )
                                + "\n"
                );
            }

            output.append(
                    "Correct answer: "
                            + formatQuizOption(
                                    question,
                                    result.getCorrectOptionIndex()
                            )
                            + "\n"
            );

            output.append(
                    "Source: "
                            + question.getSourceId()
                            + "\n\n"
            );
        }

        return output.toString();
    }

    private String formatQuizOption(
            Question question,
            Integer optionIndex) {

        if (optionIndex == null) {

            return "[skipped]";
        }

        String letter =
                switch (optionIndex) {

                    case 0 -> "A";
                    case 1 -> "B";
                    case 2 -> "C";
                    case 3 -> "D";

                    default ->
                            "?";
                };

        return letter
                + ". "
                + question
                        .getOptions()
                        .get(optionIndex);
    }

    // =====================================================
    // F08 - OPTIONAL AI FEEDBACK
    // =====================================================

    private void requestMistakeFeedback(
            JButton generateButton,
            JTextArea outputArea,
            Quiz quiz,
            QuizAttempt attempt,
            String deterministicOutput) {

        statusLabel.setText(
                "Quiz graded - generating mistake feedback..."
        );

        SwingWorker<Map<UUID, String>, Void> worker =
                new SwingWorker<>() {

                    @Override
                    protected Map<UUID, String> doInBackground()
                            throws Exception {

                        return learningAgent
                                .explainMistakes(
                                        quiz,
                                        attempt
                                );
                    }

                    @Override
                    protected void done() {

                        generateButton.setEnabled(
                                true
                        );

                        StringBuilder output =
                                new StringBuilder(
                                        deterministicOutput
                                );

                        output.append(
                                "\n\nMISTAKE FEEDBACK\n"
                        );

                        output.append(
                                "==============================\n\n"
                        );

                        try {

                            Map<UUID, String> feedback =
                                    get();

                            if (feedback.isEmpty()) {

                                output.append(
                                        "No AI feedback was available.\n"
                                );

                                output.append(
                                        "The deterministic score remains unchanged."
                                );

                            } else {

                                List<Question> questions =
                                        quiz.getQuestions();

                                List<QuestionResult> results =
                                        attempt.getResults();

                                for (int i = 0;
                                     i < results.size();
                                     i++) {

                                    QuestionResult result =
                                            results.get(i);

                                    if (result.isCorrect()) {

                                        continue;
                                    }

                                    Question question =
                                            questions.get(i);

                                    output.append(
                                            "Question "
                                                    + (i + 1)
                                                    + ":\n"
                                    );

                                    String explanation =
                                            feedback.get(
                                                    question.getId()
                                            );

                                    if (explanation == null
                                            || explanation.isBlank()) {

                                        output.append(
                                                "No AI explanation was returned.\n\n"
                                        );

                                    } else {

                                        output.append(
                                                explanation
                                                        + "\n\n"
                                        );
                                    }
                                }

                                output.append(
                                        "AI feedback is explanatory only.\n"
                                );

                                output.append(
                                        "The stored deterministic score was not changed."
                                );
                            }

                        } catch (InterruptedException ex) {

                            Thread.currentThread()
                                    .interrupt();

                            output.append(
                                    "AI feedback was interrupted.\n"
                            );

                            output.append(
                                    "The deterministic score remains unchanged."
                            );

                        } catch (ExecutionException ex) {

                            Throwable cause =
                                    ex.getCause();

                            output.append(
                                    "AI feedback could not be generated.\n"
                            );

                            output.append(
                                    "Reason: "
                                            + (
                                            cause == null
                                                    ? safeMessage(ex)
                                                    : safeMessage(cause)
                                    )
                                            + "\n\n"
                            );

                            output.append(
                                    "The deterministic score remains unchanged."
                            );
                        }

                        outputArea.setText(
                                output.toString()
                        );

                        outputArea.setCaretPosition(
                                0
                        );

                        statusLabel.setText(
                                "Quiz graded: "
                                        + attempt.getCorrectCount()
                                        + "/"
                                        + attempt.getGradedCount()
                                        + " (progress updated)"
                        );
                    }
                };

        worker.execute();
    }

    private void showQuizError(
            JTextArea outputArea,
            String message) {

        outputArea.setText(
                "QUIZ OPERATION FAILED\n"
                        + "==============================\n\n"
                        + message
        );

        statusLabel.setText(
                "Quiz operation failed"
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                "Quiz Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =====================================================
    // F09 - PROGRESS
    // =====================================================

    private JPanel createProgressPanel() {

        FeaturePanel feature =
                createFeaturePanel(
                        "Progress",
                        "View topic accuracy, evidence counts and weak-topic analysis."
                );

        JButton progressButton =
                new JButton(
                        "View Progress"
                );

        progressButton.addActionListener(
                e -> showProgressGUI(
                        feature.outputArea
                )
        );

        addButtons(
                feature,
                progressButton
        );

        return feature.panel;
    }

    private void showProgressGUI(
            JTextArea outputArea) {

        try {

            ProgressSummary summary =
                    progressTracker.analyse(
                            course.id()
                    );

            StringBuilder output =
                    new StringBuilder();

            output.append(
                    "COURSE PROGRESS\n"
            );

            output.append(
                    "==============================\n\n"
            );

            output.append(
                    "Course: "
                            + course.getName()
                            + "\n\n"
            );

            if (summary.isEmpty()) {

                output.append(
                        "No committed quiz attempts exist yet.\n\n"
                );

                output.append(
                        "Complete and grade a quiz to begin "
                                + "tracking topic progress."
                );

                outputArea.setText(
                        output.toString()
                );

                outputArea.setCaretPosition(
                        0
                );

                statusLabel.setText(
                        "No progress data available"
                );

                return;
            }

            output.append(
                    "TOPIC RESULTS\n"
            );

            output.append(
                    "==============================\n\n"
            );

            for (TopicProgress topic :
                    summary.getTopics()) {

                output.append(
                        "Topic: "
                                + topic.getTopic()
                                + "\n"
                );

                output.append(
                        "Correct: "
                                + topic.getCorrectCount()
                                + " / "
                                + topic.getGradedCount()
                                + "\n"
                );

                output.append(
                        "Accuracy: "
                                + String.format(
                                        "%.1f",
                                        topic.getAccuracyPercent()
                                )
                                + "%\n"
                );

                output.append(
                        "Status: "
                                + topic.getStatus()
                                + "\n"
                );

                if (!topic.hasEnoughEvidence()) {

                    output.append(
                            "Evidence: fewer than 3 graded "
                                    + "questions - insufficient data.\n"
                    );

                } else if (topic.isWeak()) {

                    output.append(
                            "Assessment: weak topic "
                                    + "(accuracy below 60%).\n"
                    );

                } else {

                    output.append(
                            "Assessment: sufficient evidence; "
                                    + "not currently weak.\n"
                    );
                }

                output.append(
                        "\n"
                );
            }

            output.append(
                    "==============================\n"
            );

            output.append(
                    "WEAK TOPICS\n"
            );

            output.append(
                    "==============================\n\n"
            );

            List<TopicProgress> weakTopics =
                    summary.getWeakTopics();

            if (weakTopics.isEmpty()) {

                output.append(
                        "No topics currently meet "
                                + "the weak-topic rule.\n"
                );

            } else {

                int number =
                        1;

                for (TopicProgress topic :
                        weakTopics) {

                    output.append(
                            number
                                    + ". "
                                    + topic.getTopic()
                                    + " - "
                                    + String.format(
                                            "%.1f",
                                            topic.getAccuracyPercent()
                                    )
                                    + "% ("
                                    + topic.getCorrectCount()
                                    + "/"
                                    + topic.getGradedCount()
                                    + ")\n"
                    );

                    number++;
                }
            }

            output.append(
                    "\n==============================\n"
            );

            output.append(
                    "INSUFFICIENT DATA\n"
            );

            output.append(
                    "==============================\n\n"
            );

            List<TopicProgress> insufficient =
                    summary.getInsufficientDataTopics();

            if (insufficient.isEmpty()) {

                output.append(
                        "No topics are currently waiting "
                                + "for additional evidence.\n"
                );

            } else {

                for (TopicProgress topic :
                        insufficient) {

                    output.append(
                            "- "
                                    + topic.getTopic()
                                    + ": "
                                    + topic.getGradedCount()
                                    + " graded question(s)\n"
                    );
                }
            }

            output.append(
                    "\n==============================\n"
            );

            output.append(
                    "Rule: a topic becomes WEAK only when "
                            + "it has at least 3 graded questions "
                            + "and accuracy is below 60%.\n"
            );

            output.append(
                    "Viewing progress does not modify stored attempts."
            );

            outputArea.setText(
                    output.toString()
            );

            outputArea.setCaretPosition(
                    0
            );

            statusLabel.setText(
                    "Progress loaded successfully"
            );

        } catch (Exception ex) {

            outputArea.setText(
                    "PROGRESS ANALYSIS FAILED\n"
                            + "==============================\n\n"
                            + safeMessage(ex)
            );

            statusLabel.setText(
                    "Progress analysis failed"
            );

            JOptionPane.showMessageDialog(
                    this,
                    safeMessage(ex),
                    "Progress Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // =====================================================
    // SHARED FEATURE PANEL
    // =====================================================

    private FeaturePanel createFeaturePanel(
            String title,
            String description) {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        20,
                        20,
                        20,
                        20
                )
        );

        JPanel headingPanel =
                new JPanel(
                        new GridLayout(
                                2,
                                1,
                                0,
                                5
                        )
                );

        JLabel heading =
                new JLabel(
                        title
                );

        heading.setFont(
                new Font(
                        Font.SANS_SERIF,
                        Font.BOLD,
                        22
                )
        );

        JLabel descriptionLabel =
                new JLabel(
                        description
                );

        headingPanel.add(
                heading
        );

        headingPanel.add(
                descriptionLabel
        );

        JPanel buttonPanel =
                new JPanel(
                        new GridLayout(
                                0,
                                3,
                                10,
                                10
                        )
                );

        JPanel top =
                new JPanel(
                        new BorderLayout(
                                10,
                                10
                        )
                );

        top.add(
                headingPanel,
                BorderLayout.NORTH
        );

        top.add(
                buttonPanel,
                BorderLayout.CENTER
        );

        panel.add(
                top,
                BorderLayout.NORTH
        );

        JTextArea outputArea =
                new JTextArea();

        outputArea.setEditable(
                false
        );

        outputArea.setLineWrap(
                true
        );

        outputArea.setWrapStyleWord(
                true
        );

        outputArea.setFont(
                new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        14
                )
        );

        outputArea.setText(
                "Select an action above."
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        outputArea
                );

        scrollPane.setBorder(
                BorderFactory.createTitledBorder(
                        "Output"
                )
        );

        panel.add(
                scrollPane,
                BorderLayout.CENTER
        );

        return new FeaturePanel(
                panel,
                buttonPanel,
                outputArea
        );
    }

    private void addButtons(
            FeaturePanel feature,
            JButton... buttons) {

        for (JButton button :
                buttons) {

            button.setPreferredSize(
                    new Dimension(
                            180,
                            40
                    )
            );

            feature.buttonPanel.add(
                    button
            );
        }
    }

    // =====================================================
    // AI ERROR
    // =====================================================

    private void showAITaskError(
            JTextArea outputArea,
            String message) {

        outputArea.setText(
                "AI OPERATION FAILED\n"
                        + "==============================\n\n"
                        + message
        );

        statusLabel.setText(
                "AI operation failed"
        );

        JOptionPane.showMessageDialog(
                this,
                message,
                "StudyMate AI Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    // =====================================================
    // STATUS BAR
    // =====================================================

    private JPanel createStatusBar() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        5,
                        15,
                        10,
                        15
                )
        );

        statusLabel =
                new JLabel(
                        "Ready"
                );

        panel.add(
                statusLabel,
                BorderLayout.WEST
        );

        return panel;
    }

    // =====================================================
    // ERROR HELPER
    // =====================================================

    private String safeMessage(
            Throwable error) {

        if (error == null) {

            return "Unknown error.";
        }

        if (error.getMessage() != null
                && !error.getMessage().isBlank()) {

            return error.getMessage();
        }

        return error
                .getClass()
                .getSimpleName();
    }

    // =====================================================
    // INTERNAL GUI VALUES
    // =====================================================

    private static class FeaturePanel {

        private final JPanel panel;
        private final JPanel buttonPanel;
        private final JTextArea outputArea;

        private FeaturePanel(
                JPanel panel,
                JPanel buttonPanel,
                JTextArea outputArea) {

            this.panel =
                    panel;

            this.buttonPanel =
                    buttonPanel;

            this.outputArea =
                    outputArea;
        }
    }

    private static class PlanInput {

        private final LocalDate examDate;
        private final List<AvailabilitySlot> availability;
        private final SchedulingStrategy strategy;

        private PlanInput(
                LocalDate examDate,
                List<AvailabilitySlot> availability,
                SchedulingStrategy strategy) {

            this.examDate =
                    examDate;

            this.availability =
                    availability;

            this.strategy =
                    strategy;
        }
    }

    // =====================================================
    // ENTRY POINT
    // =====================================================

    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(
                () -> {

                    try {

                        UIManager.setLookAndFeel(
                                UIManager
                                        .getSystemLookAndFeelClassName()
                        );

                    } catch (Exception ignored) {
                    }

                    try {

                        StudyGUI gui =
                                new StudyGUI();

                        gui.setVisible(
                                true
                        );

                    } catch (Exception e) {

                        JOptionPane.showMessageDialog(
                                null,
                                "StudyMate could not start:\n"
                                        + (
                                        e.getMessage() == null
                                                ? e.getClass()
                                                        .getSimpleName()
                                                : e.getMessage()
                                ),
                                "StudyMate Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
        );
    }
}