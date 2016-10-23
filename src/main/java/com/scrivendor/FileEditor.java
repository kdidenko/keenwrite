/*
 * Copyright (c) 2015 Karl Tauber <karl at jformdesigner dot com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.scrivendor;

import com.scrivendor.editor.MarkdownEditorPane;
import com.scrivendor.preview.MarkdownPreviewPane;
import com.scrivendor.service.Options;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import org.fxmisc.undo.UndoManager;

/**
 * Editor for a single file.
 *
 * @author Karl Tauber
 */
class FileEditor {

  private final Options options = Services.load( Options.class );

  private final MainWindow mainWindow;
  private final Tab tab = new Tab();
  private MarkdownEditorPane markdownEditorPane;
  private MarkdownPreviewPane markdownPreviewPane;

  private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
  private final ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();
  private final BooleanProperty canUndo = new SimpleBooleanProperty();
  private final BooleanProperty canRedo = new SimpleBooleanProperty();

  FileEditor( final MainWindow mainWindow, final Path path ) {
    this.mainWindow = mainWindow;
    this.path.set( path );

    // avoid that this is GCed
    tab.setUserData( this );

    this.path.addListener( (observable, oldPath, newPath) -> updateTab() );
    this.modified.addListener( (observable, oldPath, newPath) -> updateTab() );
    updateTab();

    tab.setOnSelectionChanged( e -> {
      if( tab.isSelected() ) {
        Platform.runLater( () -> activated() );
      }
    } );
  }

  Tab getTab() {
    return tab;
  }

  MarkdownEditorPane getEditor() {
    return markdownEditorPane;
  }


  Path getPath() {
    return path.get();
  }

  void setPath( Path path ) {
    this.path.set( path );
  }

  ObjectProperty<Path> pathProperty() {
    return path;
  }

  boolean isModified() {
    return modified.get();
  }

  ReadOnlyBooleanProperty modifiedProperty() {
    return modified.getReadOnlyProperty();
  }


  BooleanProperty canUndoProperty() {
    return canUndo;
  }


  BooleanProperty canRedoProperty() {
    return canRedo;
  }

  private void updateTab() {
    Path filePath = this.path.get();
    tab.setText( (filePath != null) ? filePath.getFileName().toString() : Messages.get( "FileEditor.untitled" ) );
    tab.setTooltip( (filePath != null) ? new Tooltip( filePath.toString() ) : null );
    tab.setGraphic( isModified() ? new Text( "*" ) : null );
  }

  private void activated() {
    if( tab.getTabPane() == null || !tab.isSelected() ) {
      return; // tab is already closed or no longer active
    }

    if( tab.getContent() != null ) {
      markdownEditorPane.requestFocus();
      return;
    }

    // load file and create UI when the tab becomes visible the first time
    markdownEditorPane = new MarkdownEditorPane();
    markdownPreviewPane = new MarkdownPreviewPane();

    markdownEditorPane.pathProperty().bind( path );

    load();

    // clear undo history after first load
    markdownEditorPane.getUndoManager().forgetHistory();

    // bind preview to editor
    markdownPreviewPane.pathProperty().bind( pathProperty() );
    markdownPreviewPane.markdownASTProperty().bind( markdownEditorPane.markdownASTProperty() );
    markdownPreviewPane.scrollYProperty().bind( markdownEditorPane.scrollYProperty() );

    // bind the editor undo manager to the properties
    UndoManager undoManager = markdownEditorPane.getUndoManager();
    modified.bind( Bindings.not( undoManager.atMarkedPositionProperty() ) );
    canUndo.bind( undoManager.undoAvailableProperty() );
    canRedo.bind( undoManager.redoAvailableProperty() );

    SplitPane splitPane = new SplitPane(
      markdownEditorPane.getNode(),
      markdownPreviewPane.getNode() );
    tab.setContent( splitPane );

    markdownEditorPane.requestFocus();
  }

  void load() {
    final Path filePath = this.path.get();

    if( filePath != null ) {
      try {
        byte[] bytes = Files.readAllBytes( filePath );

        String markdown;

        try {
          markdown = new String( bytes, getOptions().getEncoding() );
        } catch( Exception e ) {
          // Unsupported encodings and null pointers will fallback here.
          markdown = new String( bytes );
        }

        markdownEditorPane.setMarkdown( markdown );
        markdownEditorPane.getUndoManager().mark();
      } catch( IOException ex ) {
        Alert alert = mainWindow.createAlert( AlertType.ERROR,
          Messages.get( "FileEditor.loadFailed.title" ),
          Messages.get( "FileEditor.loadFailed.message" ), filePath, ex.getMessage() );
        alert.showAndWait();
      }
    }
  }

  boolean save() {
    final String markdown = markdownEditorPane.getMarkdown();

    byte[] bytes;

    try {
      bytes = markdown.getBytes( getOptions().getEncoding() );
    } catch( Exception ex ) {
      bytes = markdown.getBytes();
    }

    try {
      Files.write( path.get(), bytes );
      markdownEditorPane.getUndoManager().mark();
      return true;
    } catch( IOException ex ) {
      Alert alert = mainWindow.createAlert( AlertType.ERROR,
        Messages.get( "FileEditor.saveFailed.title" ),
        Messages.get( "FileEditor.saveFailed.message" ), path.get(), ex.getMessage() );
      alert.showAndWait();
      return false;
    }
  }

  private Options getOptions() {
    return this.options;
  }
}