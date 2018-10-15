/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package updater;

import java.awt.Dimension;
import java.awt.Toolkit;

/**
 *
 * @author kyo
 */
public class Updater {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    // TODO code application logic here
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        UpdaterForm mainForm = new UpdaterForm();
        mainForm.setWindowsIcon(getClass().getResource("/images/vs-logo.png"));
        mainForm.setIconImage(mainForm.getWindowsIcon().getImage());                

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        //mainForm.setSize((int) (dim.width * 0.9), (int) (dim.height * 0.8));
        //mainForm.setSize( dim.width, dim.height);
        mainForm.setLocation(dim.width / 2 - mainForm.getSize().width / 2, dim.height / 2 - mainForm.getSize().height / 2);
        mainForm.setVisible(true);                
      }
    });
  }
  
}
