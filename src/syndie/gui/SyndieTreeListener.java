package syndie.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * encapsulate standard tree interaction functionality - override selected(), 
 * returnHit(), and/or resized() as necessary
 */
public class SyndieTreeListener implements KeyListener, TraverseListener, SelectionListener,
                                           ControlListener, MouseListener, TreeListener {

    private final Tree _tree;

    public SyndieTreeListener(Tree tree) { _tree = tree; }
    
    /** the tree's selection was updated */
    public void selected() {}
    /** the user doubleclicked on the selected row */
    public void doubleclick() {}
    /** the user hit return on the selected row */
    public void returnHit() {}
    /** the tree was resized */
    public void resized() {}
    /** always expand on return, but we may not always want to collapse on return.  default is to do so though */
    public boolean collapseOnReturn() { return true; }
    /** when traversing by the keyboard, position us horizontally at the beginning of the row */
    public boolean keyToFirstColumn() { return true; }
    public void selectionUpdated() {}
    /** the delete key was hit */
    public void deleteHit() {}
    
    // remaining are the implementations of the listeners
    
    protected void selected(SelectionEvent evt) { selectionUpdated(); selected(); }
    
    public void widgetDefaultSelected(SelectionEvent evt) { selected(evt); }
    public void widgetSelected(SelectionEvent evt) { selected(evt); }
    
    public void keyTraversed(TraverseEvent evt) {
        _tree.setRedraw(false);
        if (evt.detail == SWT.TRAVERSE_RETURN) {
            TreeItem selected = getSelected();
            if (selected != null && collapseOnReturn() && selected.getExpanded() && (selected.getItemCount() > 0)) {
                boolean expand = !selected.getExpanded();
                selected.setExpanded(expand);
                if (expand)
                    expanded();
                else
                    collapsed();
            }
            returnHit();
        }
        selectionUpdated();
        if (keyToFirstColumn())
            scrollHorizontalBeginning();
        _tree.setRedraw(true);
    }
    
    public void keyPressed(KeyEvent keyEvent) {}
    public void keyReleased(KeyEvent evt) {
        TreeItem selected = getSelected();
        if (selected == null) return;
        
        _tree.setRedraw(false);
        
        selectionUpdated();
        if (evt.keyCode == SWT.ARROW_LEFT) {
            if (selected.getExpanded()) {
                selected.setExpanded(false);
                collapsed();
            } else {
                TreeItem parent = selected.getParentItem();
                if (parent != null) {
                    parent.setExpanded(false);
                    collapsed();
                    _tree.setSelection(parent);
                }
            }
        } else if (evt.keyCode == SWT.ARROW_RIGHT) {
            selected.setExpanded(true);
            expanded();
        } else if (evt.character == ' ') {
            boolean expand = !selected.getExpanded();
            selected.setExpanded(expand);
            if (expand)
                expanded();
            else
                collapsed();
        } else if (evt.keyCode == SWT.DEL) {
            deleteHit();
        }
        if (keyToFirstColumn())
            scrollHorizontalBeginning();
        _tree.setRedraw(true);
    }

    public void controlMoved(ControlEvent controlEvent) {}
    public void controlResized(ControlEvent evt) { resized(); }

    public void mouseDoubleClick(MouseEvent evt) { doubleclick(); }
    public void mouseDown(MouseEvent evt) {
        selectionUpdated();
    }
    public void mouseUp(MouseEvent evt) {}

    /** @since 1.102b-10 */
    public void treeCollapsed(TreeEvent evt) { collapsed(); }

    /** @since 1.102b-10 */
    public void treeExpanded(TreeEvent evt) { expanded(); }

    /** override to get notified of collapse events */
    public void collapsed() {}

    /** override to get notified of expand events */
    public void expanded() {}
    
    private void scrollHorizontalBeginning() {
        ScrollBar bar = _tree.getHorizontalBar();
        if (bar != null && !bar.isDisposed())
            bar.setSelection(bar.getMinimum());
    }
    
    protected TreeItem getSelected() { if (_tree.getSelectionCount() > 0) return _tree.getSelection()[0]; return null; }
}
