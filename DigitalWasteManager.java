
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;

public class DigitalWasteManager {

    private List<File> files = new ArrayList<>();
    private JTextArea output = new JTextArea();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DigitalWasteManager::new);
    }

    public DigitalWasteManager() {
        JFrame frame = new JFrame("Digital Waste Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 550);

        JButton selectBtn = new JButton("Select Folder");
        JButton dupBtn = new JButton("Find Duplicates");
        JButton obsBtn = new JButton("Find Obsolete Files");
        JButton delDupBtn = new JButton("Delete Duplicates");
        JButton delObsBtn = new JButton("Delete Obsolete Files");

        output.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(output);

        JPanel panel = new JPanel();
        panel.add(selectBtn);
        panel.add(dupBtn);
        panel.add(obsBtn);
        panel.add(delDupBtn);
        panel.add(delObsBtn);

        frame.getContentPane().add(panel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.setVisible(true);

        selectBtn.addActionListener(e -> chooseFolder());
        dupBtn.addActionListener(e -> showDuplicates(false));
        obsBtn.addActionListener(e -> showObsolete(false));
        delDupBtn.addActionListener(e -> showDuplicates(true));
        delObsBtn.addActionListener(e -> showObsolete(true));
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int res = chooser.showOpenDialog(null);
        if (res == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            files.clear();
            scanFolder(folder);
            output.setText("Scanned " + files.size() + " files in: " + folder.getAbsolutePath() + "\n");
        }
    }

    private void scanFolder(File folder) {
        File[] list = folder.listFiles();
        if (list != null) {
            for (File file : list) {
                if (file.isDirectory()) {
                    scanFolder(file);
                } else {
                    files.add(file);
                }
            }
        }
    }

    private void showDuplicates(boolean delete) {
        Map<String, List<File>> hashMap = new HashMap<>();
        output.setText(delete ? "Deleting Duplicate Files:\n\n" : "Duplicate Files:\n\n");

        for (File file : files) {
            try {
                String hash = getFileHash(file.toPath());
                hashMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
            } catch (Exception e) {
                output.append("Error reading file: " + file.getAbsolutePath() + "\n");
            }
        }

        boolean found = false;
        for (List<File> dupGroup : hashMap.values()) {
            if (dupGroup.size() > 1) {
                found = true;
                output.append("Group:\n");
                for (int i = 0; i < dupGroup.size(); i++) {
                    File f = dupGroup.get(i);
                    if (delete && i > 0 && f.delete()) {
                        output.append("Deleted: " + f.getAbsolutePath() + "\n");
                    } else {
                        output.append(" - " + f.getAbsolutePath() + "\n");
                    }
                }
                output.append("\n");
            }
        }

        if (!found) output.setText("No duplicates found.");
    }

    private void showObsolete(boolean delete) {
        long now = System.currentTimeMillis();
        long threshold = 365L * 24 * 60 * 60 * 1000;
        output.setText(delete ? "Deleting Obsolete Files:\n\n" : "Obsolete Files (>365 days old):\n\n");
        int count = 0;

        for (File file : files) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                long lastAccess = attrs.lastAccessTime().toMillis();
                if ((now - lastAccess) > threshold) {
                    count++;
                    if (delete && file.delete()) {
                        output.append("Deleted: " + file.getAbsolutePath() + "\n");
                    } else {
                        output.append(file.getAbsolutePath() + "\n");
                    }
                }
            } catch (Exception e) {
                output.append("Error accessing: " + file.getAbsolutePath() + "\n");
            }
        }

        if (count == 0) output.setText("No obsolete files found.");
    }

    private String getFileHash(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] data = Files.readAllBytes(path);
        byte[] hashBytes = digest.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
