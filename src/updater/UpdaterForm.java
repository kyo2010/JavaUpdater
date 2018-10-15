/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package updater;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.lang.System.exit;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 *
 * @author kyo
 */
public class UpdaterForm extends javax.swing.JFrame {

  public static String UPDATE_PATH = "update/";
  public static String UPDATE_OLD_PATH = "update_old/";
  ParseIniFile pif = new ParseIniFile("Updater.ini");

  String serverUri = "";
  List<UploadFileInfo> filesForUpdate = new ArrayList();

  /**
   * Creates new form UpdaterForm
   */
  public UpdaterForm() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
    }
    initComponents();
    filesForUpdate.clear();
    try {
      serverUri = pif.getParam("httpHost");
      //Setting up proxies
      Properties systemSettings = System.getProperties();
      try {
        if (pif.getParam("proxySet").equalsIgnoreCase("true")) {
          systemSettings.put("proxySet", pif.getParam("proxySet"));
          systemSettings.put("https.proxyHost", pif.getParam("proxyHost"));
          systemSettings.put("https.proxyPort", pif.getParam("proxyPort"));
        }
        if (!pif.getParam("useSystemProxies").trim().equalsIgnoreCase("")) {
          System.setProperty("java.net.useSystemProxies", pif.getParam("useSystemProxies"));
        }
      } catch (Exception e) {
      }
      //The same way we could also set proxy for http            
      System.out.println("Server uri : " + serverUri);
      new UploadTimer(serverUri + "/update.list" /*+"?time=" + Calendar.getInstance().getTimeInMillis()*/,
              UPDATE_PATH + "update.list", true).setAction(new UploaderActions() {
        @Override
        public void finishFile(UploadTimer timer, String fileName) {
          uploadNewFiles();
        }

        public void errorFile(UploadTimer timer, String fileName) {
          lInfo1.setText("Please check your connection. File:" + timer.url);
          int res = JOptionPane.showConfirmDialog(UpdaterForm.this, "Do you want to retry?", "Information", JOptionPane.YES_NO_OPTION);
          if (res == JOptionPane.NO_OPTION) {
            exit(0);
          }
          timer.start();
        }
      });
    } catch (Exception e) {
    }
  }

  public static ImageIcon windowsIcon = null;

  public static ImageIcon getWindowsIcon() {
    return windowsIcon;
  }

  public static void setWindowsIcon(URL url) {
    windowsIcon = new ImageIcon(url);
  }

  public void uploadNewFiles() {
    Map<String, UploadFileInfo> newFiles = UploadFileInfo.getFileInfo(UPDATE_PATH + "update.list");
    Map<String, UploadFileInfo> oldFiles = UploadFileInfo.getFileInfo("update.list");

    int countNewFile = 0;
    for (String fileName : newFiles.keySet()) {
      UploadFileInfo newFile = newFiles.get(fileName);
      UploadFileInfo oldFile = oldFiles.get(fileName);
      if (oldFile == null) {
        countNewFile++;
        filesForUpdate.add(newFile);
      } else {
        if (oldFile.version == null || !oldFile.version.equalsIgnoreCase(newFile.version)) {
          countNewFile++;
          filesForUpdate.add(newFile);
        }
      }
    }
    if (countNewFile == 0) {
      bCancel.setText("Ok");
      lInfo1.setText("Your version is actual. There are no updates.");
      progressBar.setValue(progressBar.getMaximum());
    } else {
      lInfo1.setText("Finding " + countNewFile + " new components");
      progressBar.setValue(0);
      progressBar.setMaximum(countNewFile);
      uploadNextNewFile();
    }
  }

  UploadFileInfo nextFile = null;

  private static boolean copyFile(File source, File dest) {
    try {
      Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  public void uploadNextNewFile() {
    if (!UpdaterForm.this.isVisible()) {
      return;
    }
    int count = 0;
    nextFile = null;
    for (UploadFileInfo fi : filesForUpdate) {
      count++;
      if (!fi.uploaded) {
        nextFile = fi;
        break;
      }
    }
    if (nextFile != null) {
      lInfo1.setText("Uploading a new component " + count + " of " + filesForUpdate.size());
      progressBar.setValue(count - 1);
      new UploadTimer(serverUri + "/" + nextFile.name, UPDATE_PATH + nextFile.name, false).setAction(new UploaderActions() {
        @Override
        public void finishFile(UploadTimer timer, String fileName) {
          nextFile.uploaded = true;
          uploadNextNewFile();
        }

        public void errorFile(UploadTimer timer, String fileName) {
          lInfo1.setText("Please check your connection. Component: '" + nextFile.name + "'");
          int res = JOptionPane.showConfirmDialog(UpdaterForm.this, "Do you want to retry?", "Information", JOptionPane.YES_NO_OPTION);
          if (res == JOptionPane.NO_OPTION) {
            exit(0);
          }
          timer.start();
        }
      });
    } else {
      bCancel.setText("Ok");
      progressBar.setValue(progressBar.getMaximum());
      lInfo1.setText("All components have been uploaded.");
      boolean updte_is_ok = true;
      for (UploadFileInfo fi : filesForUpdate) {
        File currentFile = new File(UPDATE_OLD_PATH + fi.name);
        currentFile.getParentFile().mkdirs();
        currentFile.delete();
        if (!(new File(fi.name).renameTo(currentFile)));
        File workFile = new File(fi.name);
        try {
          workFile.getParentFile().mkdirs();
        } catch (Exception e) {
        }
        //while (!(new File(UPDATE_PATH+fi.name).renameTo(workFile))) { 
        while (!copyFile(new File(UPDATE_PATH + fi.name), workFile)) {
          if (fi.name.equalsIgnoreCase("Updater.jar")) {
            break;
          }
          int res = JOptionPane.showConfirmDialog(this, "Please close the updated application.\nFile '" + fi.name + "' is locked", "Information", JOptionPane.YES_NO_OPTION);
          if (res == JOptionPane.NO_OPTION) {
            exit(0);
          }
          workFile.delete();
          try {
            workFile.getParentFile().mkdirs();
          } catch (Exception e) {
          }
        }
      }
      new File("update.list").renameTo(new File(UPDATE_OLD_PATH + "httpHost"));
      new File("update.list").delete();
      copyFile(new File(UPDATE_PATH + "update.list"), new File("update.list"));
      lInfo1.setText("All components have been updated. Please restart the application.");
    }
  }

  interface UploaderActions {

    void finishFile(UploadTimer timer, String fileName);

    void errorFile(UploadTimer timer, String fileName);
  }

  UploadTimer currentTimer = null;

  public final class UploadTimer extends Timer {

    public UploaderActions action = null;
    boolean showProgress = true;
    OutputStream outStream = null;
    public String url;

    public UploadTimer setAction(UploaderActions action) {
      this.action = action;
      return this;
    }

    public UploadTimer(final String url, final String filename, boolean showProgress) {
      super(100, null);
      currentTimer = this;
      setRepeats(false);
      this.url = url;
      this.showProgress = showProgress;
      addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
          new File(UPDATE_PATH).mkdirs();
          URLConnection connection = null;
          InputStream is = null;
          File targetFile = null;
          URL server = null;
          closeStream();
          //Setting up proxies
          /*Properties systemSettings = System.getProperties();
            systemSettings.put("proxySet", "true");
            systemSettings.put("https.proxyHost", "https proxy of my organisation");
            systemSettings.put("https.proxyPort", "8080");
            //The same way we could also set proxy for http
            System.setProperty("java.net.useSystemProxies", "true");*/
          //code to fetch file
          try {
            if (UploadTimer.this.showProgress) {
              progressBar.setValue(0);
            }
            server = new URL(url);
            String boundary = Long.toHexString(System.currentTimeMillis());
            connection = server.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept-Language", "UTF-8");
            connection.setConnectTimeout(100000);
            connection.setReadTimeout(100000);
            connection.setDoOutput(true);
            is = connection.getInputStream();
            if (UploadTimer.this.showProgress) {
              progressBar.setMaximum(is.available());
            }
            new File(filename).getParentFile().mkdirs();

            targetFile = new File(filename);
            outStream = new FileOutputStream(targetFile);
            int repeat = 0;

            boolean isError = false;
            int count = 0;
            //           do {
            byte[] buffer = new byte[1000];
            int noOfBytes = 0;
            isError = false;
            //try {
            while ((noOfBytes = is.read(buffer)) > 0) {
              count++;
              //outStream.write(buffer);
              outStream.write(buffer, 0, noOfBytes);
              if (UploadTimer.this.showProgress) {
                progressBar.setValue(1000 * count);
              }
              if (!UpdaterForm.this.isVisible()) {
                return;
              }
            }
            /*} catch (Exception e) {
                isError = true;
                repeat++;
              }*/
//            } while (isError && repeat <= 3);
//            if (isError) throw new MalformedURLException("");
            if (UploadTimer.this.action != null) {
              UploadTimer.this.action.finishFile(UploadTimer.this, filename);
            }
          } catch (MalformedURLException e) {
            if (UploadTimer.this.action != null) {
              UploadTimer.this.action.errorFile(UploadTimer.this, filename);
            }
            //JOptionPane.showMessageDialog(UpdaterForm.this, "The url is not correct", "Error", JOptionPane.INFORMATION_MESSAGE);
            e.printStackTrace();
          } catch (IOException e) {
            if (UploadTimer.this.action != null) {
              UploadTimer.this.action.errorFile(UploadTimer.this, filename);
            }
            //JOptionPane.showMessageDialog(UpdaterForm.this, "File uploading is error", "Error", JOptionPane.INFORMATION_MESSAGE);
            e.printStackTrace();
          } catch (Exception e0) {
            if (UploadTimer.this.action != null) {
              UploadTimer.this.action.errorFile(UploadTimer.this, filename);
            }
            e0.printStackTrace();
          } finally {
            closeStream();
          }
        }
      });
      setRepeats(false);
      start();
    }

    void closeStream() {
      if (outStream != null) {
        try {
          outStream.close();
        } catch (Exception ein) {
        }
        outStream = null;
      }
    }
  };

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    bCancel = new javax.swing.JButton();
    progressBar = new javax.swing.JProgressBar();
    lInfo1 = new javax.swing.JLabel();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Updater");
    setResizable(false);

    bCancel.setText("Cancel");
    bCancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        bCancelActionPerformed(evt);
      }
    });

    lInfo1.setText("   ");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 494, Short.MAX_VALUE)
          .addComponent(lInfo1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGap(0, 0, Short.MAX_VALUE)
            .addComponent(bCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(0, 0, Short.MAX_VALUE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(lInfo1)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(bCancel)
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void bCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bCancelActionPerformed
    // TODO add your handling code here:
    setVisible(false);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setVisible(false);
    exit(0);
  }//GEN-LAST:event_bCancelActionPerformed

  public void saveUrl(final String filename, final String url) throws IOException {
    //String url="https://raw.githubusercontent.com/bpjoshi/fxservice/master/src/test/java/com/bpjoshi/fxservice/api/TradeControllerTest.java";
    OutputStream outStream = null;
    URLConnection connection = null;
    InputStream is = null;
    File targetFile = null;
    URL server = null;
    //Setting up proxies
    /*Properties systemSettings = System.getProperties();
            systemSettings.put("proxySet", "true");
            systemSettings.put("https.proxyHost", "https proxy of my organisation");
            systemSettings.put("https.proxyPort", "8080");
            //The same way we could also set proxy for http
            System.setProperty("java.net.useSystemProxies", "true");*/
    //code to fetch file
    try {
      server = new URL(url);
      connection = server.openConnection();
      is = connection.getInputStream();
      byte[] buffer = new byte[is.available()];
      is.read(buffer);
      targetFile = new File(filename);
      outStream = new FileOutputStream(targetFile);
      outStream.write(buffer);
    } catch (MalformedURLException e) {
      System.out.println("THE URL IS NOT CORRECT ");
      e.printStackTrace();
    } catch (IOException e) {
      System.out.println("Io exception");
      e.printStackTrace();
    } finally {
      if (outStream != null) {
        outStream.close();
      }
    }
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    /* Set the Nimbus look and feel */
    //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
    /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
     */
    try {
      for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          javax.swing.UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (ClassNotFoundException ex) {
      java.util.logging.Logger.getLogger(UpdaterForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      java.util.logging.Logger.getLogger(UpdaterForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      java.util.logging.Logger.getLogger(UpdaterForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    } catch (javax.swing.UnsupportedLookAndFeelException ex) {
      java.util.logging.Logger.getLogger(UpdaterForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
    }
    //</editor-fold>
    //</editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new UpdaterForm().setVisible(true);
      }
    });
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton bCancel;
  private javax.swing.JLabel lInfo1;
  private javax.swing.JProgressBar progressBar;
  // End of variables declaration//GEN-END:variables
}
