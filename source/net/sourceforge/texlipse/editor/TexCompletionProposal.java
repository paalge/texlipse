package net.sourceforge.texlipse.editor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

/**
 * This class is a wrapper class to create an ICompletionProposal
 * from a TexCommandEntry. It also creates "smart" braces.
 * 
 * @author Boris von Loesch
 *
 */
public class TexCompletionProposal implements ICompletionProposal {
	private TexCommandEntry fentry;
	private int fReplacementOffset;
	private int fReplacementLength;
	private ISourceViewer fviewer;
	
	public TexCompletionProposal(TexCommandEntry entry, int replacementOffset, int replacementLength, ISourceViewer viewer) {
		fentry = entry;
		fReplacementLength = replacementLength;
		fReplacementOffset = replacementOffset;
		fviewer = viewer;
	}
	
	public void apply(IDocument document) {
		try {
	        String bracePair = "{}";
	        String braces = "";
            if (fentry.arguments > 0) {
                for (int j=0; j < fentry.arguments; j++) braces += bracePair;
    			document.replace(fReplacementOffset, fReplacementLength, fentry.key+braces);
    			if (TexlipsePlugin.getDefault().getPreferenceStore()
    					.getBoolean(TexlipseProperties.TEX_BRACKET_COMLETION)){
					LinkedModeModel model= new LinkedModeModel();
    				for (int j=0; j < fentry.arguments; j++){
    					int newOffset= fReplacementOffset + fentry.key.length() + j*2 + 1;
    					LinkedPositionGroup group= new LinkedPositionGroup();
    					group.addPosition(new LinkedPosition(document, newOffset, 0, LinkedPositionGroup.NO_STOP));
    					model.addGroup(group);
       				}
					model.forceInstall();
					LinkedModeUI ui= new EditorLinkedModeUI(model, fviewer);
					ui.setSimpleMode(false);
					ui.setExitPolicy(new ExitPolicy('}', fviewer));
					ui.setExitPosition(fviewer, fReplacementOffset + fentry.key.length() + braces.length(), 
							0, Integer.MAX_VALUE);
					ui.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
					ui.enter();
    			}
            }
            else
    			document.replace(fReplacementOffset, fReplacementLength, fentry.key);
		} catch (BadLocationException x) {
			// ignore
		}
	}

	public Point getSelection(IDocument document) {
		if (fentry.arguments > 0) 
			return new Point(fReplacementOffset + fentry.key.length()+1, 0);
		return new Point(fReplacementOffset + fentry.key.length(), 0);
	}

	public String getAdditionalProposalInfo() {
        return (fentry.info.length() > TexCompletionProcessor.assistLineLength ?
                TexCompletionProcessor.wrapString(fentry.info, TexCompletionProcessor.assistLineLength)
                : fentry.info);
		//return fentry.info;
	}

	public String getDisplayString() {
		return fentry.key;
	}

	public Image getImage() {
		if (fentry.image != null) return fentry.image.createImage();
		return null;
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	protected static class ExitPolicy implements IExitPolicy {

		final char fExitCharacter;
		ISourceViewer fviewer;

		public ExitPolicy(char exitCharacter, ISourceViewer viewer) {
			fExitCharacter= exitCharacter;
			fviewer = viewer;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI.ExitPolicy#doExit(org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager, org.eclipse.swt.events.VerifyEvent, int, int)
		 */
		public ExitFlags doExit(LinkedModeModel environment, VerifyEvent event, int offset, int length) {
			if (event.character == fExitCharacter) {
				try {
					if (fviewer.getDocument().getChar(offset) == fExitCharacter)
					event.doit = false;
					try {
						if (fviewer.getDocument().getChar(offset + 1) == '{')
							fviewer.setSelectedRange(offset + 2, 0);
						else fviewer.setSelectedRange(offset + 1, 0);
						return null;
					} catch (BadLocationException e) {
						//
					}
					fviewer.setSelectedRange(offset + 1, 0);
				} catch (BadLocationException e1) {
					//Should not happen
				}
			}
			else if (event.character == '{') {
				try {
					if (fviewer.getDocument().getChar(offset - 1) == '{')
						event.doit = false;
				} catch (BadLocationException e) {
					//
				}
			}
			return null;
		}
	}

}