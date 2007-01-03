package syndie.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.i2p.data.Hash;
import net.i2p.data.SigningPublicKey;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import syndie.Constants;
import syndie.data.ArchiveInfo;
import syndie.data.ChannelInfo;
import syndie.data.ReferenceNode;
import syndie.data.SyndieURI;

/**
 *
 */
class ViewForum implements Translatable, Themeable {
    private BrowserControl _browser;
    private Composite _parent;
    private SyndieURI _uri;
    private Hash _scope;
    private long _scopeId;
    
    private ScrolledComposite _scroll;
    private Composite _root;
    private ImageCanvas _avatar;
    private Image _avatarImgOrig;
    private Image _avatarImg;
    private List _avatarImgStandard;
    private MenuItem _avatarOther;
    private Label _nameLabel;
    private Text _name;
    private Label _tagsLabel;
    private Text _tags;
    private Label _descriptionLabel;
    private Text _description;
    private Button _avatarSelect;
    private Menu _avatarMenu;
    private Label _authorizationLabel;
    private Combo _authorization;
    private Label _expirationLabel;
    private Text _expiration;
    private Label _referencesLabel;
    private Combo _references;
    /** ReferenceNode instances correlating with the entries in _references (may be null) */
    private List _referenceNodes;
    /** just the roots of the _references */
    private List _referenceNodeRoots;
    private Group _userGroup;
    private Table _users;
    private TableColumn _userName;
    private TableColumn _userPriv;
    private Map _userItemToHash;
    private Button _userAdd;
    private Group _archiveGroup;
    private Table _archives;
    private TableColumn _archiveURL;
    private TableColumn _archiveIsPub;
    private Map _archiveItemToURI;
    private Button _archiveAdd;
    private Composite _actions;
    private Button _save;
    private Button _cancel;
    private Group _keyManagementGroup;
    private Button _keyManagementOpen;
    private Label _keyManagementOpenInfo;
    private Button _keyManagementKeep;
    private Label _keyManagementKeepInfo;
    private Button _keyManagementRotate;
    private Label _keyManagementRotateInfo;
    private Button _keyManagementReset;
    private Label _keyManagementResetInfo;
    private Button _keyManagementPBE;
    private Label _keyManagementPBEInfo;
    private Button _keyManagementNewReply;
    
    private boolean _editable;
    private boolean _initialized;
    private boolean _modified;
    
    private ManageReferenceChooserPopup _refPopup;
    
    private List _managerHashes;
    private List _posterHashes;
    private List _pubArchiveURIs;
    private List _privArchiveURIs;
    private String _passphrase;
    private String _prompt;
    
    public ViewForum(BrowserControl browser, Composite parent, SyndieURI uri) {
        _browser = browser;
        _parent = parent;
        _uri = uri;
        _editable = false;
        _scope = null;
        _scopeId = -1;
        _initialized = false;
        _avatarImgStandard = new ArrayList();
        _archiveItemToURI = new HashMap();
        _userItemToHash = new HashMap();
        Hash scope = uri.getScope();
        if (scope == null)
            scope = uri.getHash("channel");
        if (scope != null) {
            List keys = browser.getClient().getNymKeys(scope, Constants.KEY_FUNCTION_MANAGE);
            if ( (keys != null) && (keys.size() > 0) )
                _editable = true;
            _browser.getUI().debugMessage("management nym keys for " + scope.toBase64() + ": " + keys);
            _scope = scope;
            _scopeId = browser.getClient().getChannelId(scope);
            
            if (_editable) {
                Long val = uri.getLong("editable");
                if ( (val != null) && (val.longValue() == 0) )
                    _editable = false;
            }
        } else {
            _browser.getUI().debugMessage("no scope!");
        }
        initComponents();
    }
    public boolean getEditable() { return _editable; }
    
    private void initComponents() {
        _scroll = new ScrolledComposite(_parent, SWT.V_SCROLL | SWT.H_SCROLL);
        _scroll.setAlwaysShowScrollBars(false);
        _scroll.setExpandHorizontal(true);
        _scroll.setExpandVertical(true);
        _root = new Composite(_scroll, SWT.NONE);
        _root.setLayout(new GridLayout(7, false));
        _scroll.setContent(_root);
        
        _parent.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent controlEvent) {}
            public void controlResized(ControlEvent controlEvent) { 
                // applyTheme does our scroll resizing
                applyTheme(_browser.getThemeRegistry().getTheme()); 
            }
        });
        
        loadOrigAvatar();
        
        _avatar = new ImageCanvas(_root, false);
        _avatar.setLayoutData(new GridData(GridData.CENTER, GridData.END, false, false, 1, 3));
        
        _nameLabel = new Label(_root, SWT.NONE);
        _nameLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _name = new Text(_root, SWT.BORDER | SWT.SINGLE | (!_editable ? SWT.READ_ONLY : 0));
        GridData gd = new GridData(GridData.FILL, GridData.FILL, true, false);
        //gd.widthHint = 100;
        _name.setLayoutData(gd);
        _name.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent evt) { modified(); } });
        
        _tagsLabel = new Label(_root, SWT.NONE);
        _tagsLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _tags = new Text(_root, SWT.BORDER | SWT.SINGLE | (!_editable ? SWT.READ_ONLY : 0));
        _tags.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        _tags.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent evt) { modified(); } });
        
        _descriptionLabel = new Label(_root, SWT.NONE);
        _descriptionLabel.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, false, false));
        
        _description = new Text(_root, SWT.BORDER | SWT.SINGLE | (!_editable ? SWT.READ_ONLY : 0));
        _description.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 5, 1));
        _description.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent evt) { modified(); } });
        
        _authorizationLabel = new Label(_root, SWT.NONE);
        _authorizationLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _authorization = new Combo(_root, SWT.READ_ONLY | SWT.DROP_DOWN);
        _authorization.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 3, 1));
        if (!_editable) _authorization.setEnabled(false);
        _authorization.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { modified(); }
            public void widgetSelected(SelectionEvent selectionEvent) { modified(); }
        });
        
        _expirationLabel = new Label(_root, SWT.NONE);
        _expirationLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _expiration = new Text(_root, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL, GridData.FILL, false, false);
        gd.widthHint = 50;
        _expiration.setLayoutData(gd);
        _expiration.addModifyListener(new ModifyListener() { public void modifyText(ModifyEvent evt) { modified(); } });
        
        if (_editable) {
            _avatarSelect = new Button(_root, SWT.PUSH);
            _avatarSelect.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
            
            _avatarMenu = new Menu(_avatar);
            _avatar.setMenu(_avatarMenu);
            
            populateAvatarMenu();
            
            _avatarSelect.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { _avatarMenu.setVisible(true); }
                public void widgetSelected(SelectionEvent selectionEvent) { _avatarMenu.setVisible(true); }
            });
        }
        
        _referencesLabel = new Label(_root, SWT.NONE);
        _referencesLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        
        _references = new Combo(_root, SWT.DROP_DOWN | SWT.READ_ONLY);
        int colspan = 5;
        if (!_editable)
            colspan = 6;
        _references.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, colspan, 1));
        
        _references.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { viewRef(); }
            public void widgetSelected(SelectionEvent selectionEvent) { viewRef(); }
            private void viewRef() {
                if (_refPopup == null)
                    _refPopup = new ManageReferenceChooserPopup(_browser, _root.getShell(), _editable);
                _refPopup.setReferences(_referenceNodeRoots);
                _refPopup.addCloseListener(new ManageReferenceChooserPopup.CloseListener() {
                    public void closed(List refRoots) {
                        _referenceNodeRoots = refRoots;
                        modified();
                        redrawReferences();
                    }
                });
                _refPopup.show();
            }
        });
        
        _userGroup = new Group(_root, SWT.SHADOW_ETCHED_IN);
        _userGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 7, 1));
        _userGroup.setLayout(new GridLayout(2, false));
        
        _users = new Table(_userGroup, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        _users.setHeaderVisible(false);
        _users.setLinesVisible(true);
        gd = new GridData(GridData.FILL, GridData.FILL, true, true);
        gd.heightHint = 50;
        _users.setLayoutData(gd);
        
        _userName = new TableColumn(_users, SWT.LEFT);
        _userPriv = new TableColumn(_users, SWT.RIGHT);
        
        final Menu userMenu = new Menu(_users);
        _users.setMenu(userMenu);
        MenuItem viewForum = new MenuItem(userMenu, SWT.PUSH);
        viewForum.setText(_browser.getTranslationRegistry().getText(T_USER_VIEWFORUM, "View forum"));
        viewForum.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { viewUser(); }
            public void widgetSelected(SelectionEvent selectionEvent) { viewUser(); }
            private void viewUser() {
                TableItem items[] = _users.getSelection();
                for (int i = 0; i < items.length; i++) {
                    Hash scope = (Hash)_userItemToHash.get(items[i]);
                    _browser.view(SyndieURI.createScope(scope));
                }
            }
        });
        MenuItem viewMeta = new MenuItem(userMenu, SWT.PUSH);
        viewMeta.setText(_browser.getTranslationRegistry().getText(T_USER_VIEWFORUMMETA, "View forum metadata"));
        viewMeta.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { viewMeta(); }
            public void widgetSelected(SelectionEvent selectionEvent) { viewMeta(); }
            private void viewMeta() {
                TableItem items[] = _users.getSelection();
                for (int i = 0; i < items.length; i++) {
                    Hash scope = (Hash)_userItemToHash.get(items[i]);
                    _browser.view(_browser.createMetaURI(scope));
                }
            }
        });

        if (_editable) {
            final MenuItem manager = new MenuItem(userMenu, SWT.PUSH);
            manager.setText(_browser.getTranslationRegistry().getText(T_USER_MANAGE, "Toggle manager/poster"));
            manager.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent evt) { toggleManager(); }
                public void widgetSelected(SelectionEvent evt) { toggleManager(); }
                private void toggleManager() {
                    TableItem items[] = _users.getSelection();
                    for (int i = 0; i < items.length; i++) {
                        Hash scope = (Hash)_userItemToHash.get(items[i]);
                        toggleUserManagement(scope);
                    }
                }
            });
            
            MenuItem delete = new MenuItem(userMenu, SWT.PUSH);
            delete.setText(_browser.getTranslationRegistry().getText(T_USER_DELETE, "Delete"));
            delete.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { delete(); }
                public void widgetSelected(SelectionEvent selectionEvent) { delete(); }
                private void delete() {
                    TableItem items[] = _users.getSelection();
                    for (int i = 0; i < items.length; i++) {
                        Hash scope = (Hash)_userItemToHash.get(items[i]);
                        deleteUser(scope);
                    }
                }
            });
            
            _userAdd = new Button(_userGroup, SWT.PUSH);
            _userAdd.setFont(_browser.getThemeRegistry().getTheme().BUTTON_FONT);
            _userAdd.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
            _userAdd.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { addUser(); }
                public void widgetSelected(SelectionEvent selectionEvent) { addUser(); }
            });
        }
        
        _archiveGroup = new Group(_root, SWT.SHADOW_ETCHED_IN);
        _archiveGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 7, 1));
        _archiveGroup.setLayout(new GridLayout(2, false));
        
        _archives = new Table(_archiveGroup, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
        _archives.setLinesVisible(true);
        _archives.setHeaderVisible(false);
        gd = new GridData(GridData.FILL, GridData.FILL, true, true);
        gd.heightHint = 50;
        _archives.setLayoutData(gd);
        
        _archiveURL = new TableColumn(_archives, SWT.LEFT);
        _archiveIsPub = new TableColumn(_archives, SWT.RIGHT);

        final Menu archiveMenu = new Menu(_archives);
        _archives.setMenu(archiveMenu);
        MenuItem view = new MenuItem(archiveMenu, SWT.PUSH);
        view.setText(_browser.getTranslationRegistry().getText(T_ARCHIVE_VIEW, "View"));
        view.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { viewArchive(); }
            public void widgetSelected(SelectionEvent selectionEvent) { viewArchive(); }
            private void viewArchive() {
                TableItem items[] = _archives.getSelection();
                for (int i = 0; i < items.length; i++) {
                    SyndieURI uri = (SyndieURI)_archiveItemToURI.get(items[i]);
                    if (uri != null) {
                        if (uri.isArchive())
                            _browser.view(uri);
                        else
                            _browser.view(SyndieURI.createArchive(uri.getURL(), null));
                    }
                }
            }
        });
        if (_editable) {
            MenuItem delete = new MenuItem(archiveMenu, SWT.PUSH);
            delete.setText(_browser.getTranslationRegistry().getText(T_ARCHIVE_DELETE, "Delete"));
            delete.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { deleteArchive(); }
                public void widgetSelected(SelectionEvent selectionEvent) { deleteArchive(); }
                private void deleteArchive() {
                    TableItem items[] = _archives.getSelection();
                    for (int i = 0; i < items.length; i++) {
                        SyndieURI uri = (SyndieURI)_archiveItemToURI.get(items[i]);
                        _pubArchiveURIs.remove(uri);
                        _privArchiveURIs.remove(uri);
                        modified();
                    }
                    redrawArchives();
                    _root.layout(true, true);
                }
            });
            MenuItem isPub = new MenuItem(archiveMenu, SWT.PUSH);
            isPub.setText(_browser.getTranslationRegistry().getText(T_ARCHIVE_ISPUB, "Toggle share publicly"));
            isPub.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { toggleArchivePub(); }
                public void widgetSelected(SelectionEvent selectionEvent) { toggleArchivePub(); }
                private void toggleArchivePub() {
                    TableItem items[] = _archives.getSelection();
                    for (int i = 0; i < items.length; i++) {
                        SyndieURI uri = (SyndieURI)_archiveItemToURI.get(items[i]);
    
                        if (_pubArchiveURIs.contains(uri)) {
                            _pubArchiveURIs.remove(uri);
                            if (!_privArchiveURIs.contains(uri))
                                _privArchiveURIs.add(uri);
                        } else {
                            _privArchiveURIs.remove(uri);
                            if (!_pubArchiveURIs.contains(uri))
                                _pubArchiveURIs.add(uri);
                        }
                    }
                    modified();
                    redrawArchives();
                    _root.layout(true, true);
                }
            });
        }

        if (_editable) {
            _archiveAdd = new Button(_archiveGroup, SWT.PUSH);
            _archiveAdd.setFont(_browser.getThemeRegistry().getTheme().BUTTON_FONT);
            _archiveAdd.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
            _archiveAdd.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { addArchive(); }
                public void widgetSelected(SelectionEvent selectionEvent) { addArchive(); }
            });
        }
        
        _actions = new Composite(_root, SWT.NONE);
        _actions.setLayout(new FillLayout(SWT.HORIZONTAL));
        _actions.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 7, 1));
        _save = new Button(_actions, SWT.PUSH);
        _cancel = new Button(_actions, SWT.PUSH);
        _save.setEnabled(false);
        _cancel.setEnabled(false);
        
        _save.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { saveChanges(); }
            public void widgetSelected(SelectionEvent selectionEvent) { saveChanges(); }
        });
        
        _cancel.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { loadData(); }
            public void widgetSelected(SelectionEvent selectionEvent) { loadData(); }
        });
        
        if (!_editable) {
            _actions.setVisible(false);
            ((GridData)_actions.getLayoutData()).exclude = true;
        }
        
        _keyManagementGroup = new Group(_root, SWT.SHADOW_ETCHED_IN);
        _keyManagementGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 7, 1));
        RowLayout rl = new RowLayout(SWT.VERTICAL);
        rl.fill = true;
        rl.wrap = false;
        /*
        GridLayout gl = new GridLayout(1, true);
        _keyManagementGroup.setLayout(gl);
         */
        _keyManagementGroup.setLayout(rl);
        
        int width = SWT.DEFAULT; //_parent.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        //_browser.getUI().debugMessage("key management width: " + width);
        
        _keyManagementOpen = new Button(_keyManagementGroup, SWT.RADIO);
        //_keyManagementOpen.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementOpen.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementOpenInfo = new Label(_keyManagementGroup, SWT.WRAP);
        //_keyManagementOpenInfo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementOpenInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementKeep = new Button(_keyManagementGroup, SWT.RADIO);
        //_keyManagementKeep.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementKeep.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementKeepInfo = new Label(_keyManagementGroup, SWT.WRAP);
        //_keyManagementKeepInfo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementKeepInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementRotate = new Button(_keyManagementGroup, SWT.RADIO);
        //_keyManagementRotate.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementRotate.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementRotateInfo = new Label(_keyManagementGroup, SWT.WRAP);
        //_keyManagementRotateInfo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementRotateInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementReset = new Button(_keyManagementGroup, SWT.RADIO);
        //_keyManagementReset.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementReset.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementResetInfo = new Label(_keyManagementGroup, SWT.WRAP);
        //_keyManagementResetInfo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementResetInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementPBE = new Button(_keyManagementGroup, SWT.CHECK);
        //_keyManagementPBE.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementPBE.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementPBEInfo = new Label(_keyManagementGroup, SWT.WRAP);
        //_keyManagementPBEInfo.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementPBEInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementNewReply = new Button(_keyManagementGroup, SWT.CHECK);
        //_keyManagementNewReply.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
        _keyManagementNewReply.setLayoutData(new RowData(width, SWT.DEFAULT));
        
        _keyManagementKeep.setSelection(true);
        
        _keyManagementPBE.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) {
                if (_keyManagementPBE.getSelection())
                    promptForPBE();
            }
            public void widgetSelected(SelectionEvent selectionEvent) {
                if (_keyManagementPBE.getSelection())
                    promptForPBE();
            }
        });
        
        if (!_editable) {
            _keyManagementGroup.setVisible(false);
            ((GridData)_keyManagementGroup.getLayoutData()).exclude = true;
        }
    
        ChannelInfo info = _browser.getClient().getChannel(_scopeId);
        loadData();
        
        _browser.getTranslationRegistry().register(this);
        _browser.getThemeRegistry().register(this);
        
        // wait until after translate, since that populates _authorization
        if (info != null) {
            if (info.getAllowPublicPosts())
                _authorization.select(AUTH_PUBLIC);
            else if (info.getAllowPublicReplies())
                _authorization.select(AUTH_PUBREPLY);
            else
                _authorization.select(AUTH_AUTH);
        }
    }
    
    public void dispose() {
        ImageUtil.dispose(_avatarImgOrig);
        for (int i = 0; i < _avatarImgStandard.size(); i++)
            ImageUtil.dispose((Image)_avatarImgStandard.get(i));
        _browser.getTranslationRegistry().unregister(this);
        _browser.getThemeRegistry().unregister(this);
        if (_refPopup != null) _refPopup.dispose();
    }
    
    /* called when the tab is closed */
    public boolean confirmClose() {
        if (!_editable || !_modified) return true;
        MessageBox confirm = new MessageBox(_parent.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        confirm.setText(_browser.getTranslationRegistry().getText(T_CONFIRM_CLOSE_TITLE, "Confirm"));
        confirm.setMessage(_browser.getTranslationRegistry().getText(T_CONFIRM_CLOSE_MSG, "Do you want to discard these changes to the forum?"));
        int rc = confirm.open();
        if (rc == SWT.YES) {
            return true;
        } else if (rc == SWT.NO) {
            return false;
        } else {
            return false;
        }
    }    
    private static final String T_CONFIRM_CLOSE_TITLE = "syndie.gui.viewforum.close.title";
    private static final String T_CONFIRM_CLOSE_MSG = "syndie.gui.viewforum.close.msg";
    
    private void promptForPBE() {
        PassphrasePrompt prompt = new PassphrasePrompt(_browser, _root.getShell(), true);
        prompt.setPassphraseListener(new PassphrasePrompt.PassphraseListener() {
            public void promptComplete(String passphraseEntered, String promptEntered) {
                _passphrase = passphraseEntered;
                _prompt = promptEntered;
            }
            public void promptAborted() {}
        });
        prompt.open();
    }
    
    private void saveChanges() {
        ManageForumExecutor exec = new ManageForumExecutor(_browser.getClient(), _browser.getUI(), new ManageForumExecutor.ManageForumState() {
            public Image getAvatar() { return _avatarImg; }
            public String getName() { return _name.getText(); }
            public String getDescription() { return _description.getText(); }
            public long getLastEdition() {
                if (_scopeId >= 0) 
                    return _browser.getClient().getChannelVersion(_scopeId);
                else
                    return -1;
            }

            public boolean getAllowPublicPosts() { return _authorization.getSelectionIndex() == AUTH_PUBLIC; }
            public boolean getAllowPublicReplies() { return _authorization.getSelectionIndex() == AUTH_PUBREPLY; }
            public Set getPublicTags() { return Collections.EMPTY_SET; }
            public Set getAuthorizedPosters() { return getPubKeys(_posterHashes); }
            public Set getAuthorizedManagers() { return getPubKeys(_managerHashes); }
            private Set getPubKeys(List scopes) {
                Set rv = new HashSet();
                for (int i = 0; i < scopes.size(); i++) {
                    Hash scope = (Hash)scopes.get(i);
                    SigningPublicKey key = _browser.getClient().getChannelIdentKey(scope);
                    if (key != null)
                        rv.add(key);
                }
                return rv;
            }
            public String getReferences() { return (_referenceNodeRoots != null ? ReferenceNode.walk(_referenceNodeRoots) : ""); }
            public Set getPublicArchives() { return getArchives(_pubArchiveURIs); }
            public Set getPrivateArchives() { return getArchives(_privArchiveURIs); }
            private Set getArchives(List uris) {
                Set archives = new HashSet();
                for (Iterator iter = uris.iterator(); iter.hasNext(); ) {
                    SyndieURI uri = (SyndieURI)iter.next();
                    ArchiveInfo info = new ArchiveInfo(uri);
                    archives.add(info);
                }
                return archives;
            }

            public long getChannelId() { return _scopeId; }
            
            public boolean getEncryptContent() { return false; }
            public boolean getPBE() { return false; }
            public String getPassphrase() { return _passphrase; }
            public String getPassphrasePrompt() { return _prompt; }
            /** return the read keys we explicitly want to deliver in the metadata, or null/empty if we don't care */
            public List getCurrentReadKeys() { return null; }
        });
        exec.execute();
        String errs = exec.getErrors();
        if ( (errs != null) && (errs.trim().length() > 0) ) {
            MessageBox box = new MessageBox(_parent.getShell(), SWT.ICON_ERROR | SWT.OK);
            box.setText(_browser.getTranslationRegistry().getText(T_ERROR_TITLE, "Error"));
            box.setMessage(_browser.getTranslationRegistry().getText(T_ERROR_MSG, "Internal error saving the forum:") + errs);
            box.open();
        } else {
            _browser.unview(_uri);
            if (_scopeId < 0)
                _browser.view(exec.getForum());
        }
    }
    private static final String T_ERROR_TITLE = "syndie.gui.viewforum.error.title";
    private static final String T_ERROR_MSG = "syndie.gui.viewforum.error.msg";
    
    private void modified() {
        if (!_initialized) return;
        if (!_modified) {
            _save.setEnabled(true);
            _cancel.setEnabled(true);
        }
        _modified = true;
    }

    private void loadOrigAvatar() {
        byte avatar[] = _browser.getClient().getChannelAvatar(_scopeId);
        if (avatar != null) {
            Image img = ImageUtil.createImage(avatar);
            if (img != null) {
                Rectangle rect = img.getBounds();
                int width = rect.width;
                int height = rect.height;
                boolean mod = false;
                if (width > Constants.MAX_AVATAR_WIDTH) {
                    width = Constants.MAX_AVATAR_WIDTH;
                    mod = true;
                }
                if (height > Constants.MAX_AVATAR_HEIGHT) {
                    height = Constants.MAX_AVATAR_HEIGHT;
                    mod = true;
                }
                if (mod)
                    img = ImageUtil.resize(img, width, height, true);
                _avatarImgOrig = img;
            } else {
                _avatarImgOrig = null;
            }
        }
    }
    private void populateAvatarMenu() {
        if (!_editable) return;
        if (_avatarImgOrig != null) { // populated earlier in the initialization
            MenuItem origItem = new MenuItem(_avatarMenu, SWT.PUSH);
            origItem.setImage(_avatarImgOrig);
            origItem.addSelectionListener(new SelectionListener() {
                public void widgetDefaultSelected(SelectionEvent selectionEvent) { pickAvatar(_avatarImgOrig); }
                public void widgetSelected(SelectionEvent selectionEvent) { pickAvatar(_avatarImgOrig); }
            });
        }
        int i = 0;
        while (true) {
            final Image img = ImageUtil.createImageFromResource("iconAvatar" + i + ".png");
            if (img != null) {
                _avatarImgStandard.add(img);
                MenuItem item = new MenuItem(_avatarMenu, SWT.PUSH);
                item.setImage(img);
                item.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent selectionEvent) { pickAvatar(img); }
                    public void widgetSelected(SelectionEvent selectionEvent) { pickAvatar(img); }
                });
                i++;
            } else {
                break;
            }
        }
        
        _avatarOther = new MenuItem(_avatarMenu, SWT.PUSH);
        _avatarOther.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent selectionEvent) { pickAvatar(); }
            public void widgetSelected(SelectionEvent selectionEvent) { pickAvatar(); }
        });
    }
    
    private void pickAvatar(Image img) {
        Image old = _avatarImg;
        if (!_avatarImgStandard.contains(old) && (old != _avatarImgOrig) )
            _avatar.disposeImage();
        _avatarImg = img;
        _avatar.setImage(img);
        _avatar.redraw();
        if (img != _avatarImgOrig)
            modified();
    }
    private void pickAvatar() {
        FileDialog dialog = new FileDialog(_root.getShell(), SWT.SINGLE | SWT.OPEN);
        dialog.setText(_browser.getTranslationRegistry().getText(T_AVATAR_OPEN_NAME, "Select a 48x48 pixel PNG image"));
        dialog.setFilterExtensions(new String[] { "*.png" });
        dialog.setFilterNames(new String[] { _browser.getTranslationRegistry().getText(T_AVATAR_OPEN_TYPE, "PNG image") });
        String filename = dialog.open();
        if (filename != null) {
            Image img = ImageUtil.createImageFromFile(filename);
            if (img != null) {
                Rectangle bounds = img.getBounds();
                int width = bounds.width;
                int height = bounds.height;
                if (width > Constants.MAX_AVATAR_WIDTH)
                    width = Constants.MAX_AVATAR_WIDTH;
                if (height > Constants.MAX_AVATAR_HEIGHT)
                    height = Constants.MAX_AVATAR_HEIGHT;
                if ( (height != bounds.height) || (width != bounds.width) ) {
                    img = ImageUtil.resize(img, width, height, true);
                }
                final Image revamped = img;
                int idx = _avatarMenu.indexOf(_avatarOther);
                MenuItem item = new MenuItem(_avatarMenu, SWT.PUSH, idx);
                item.setImage(img);
                item.addSelectionListener(new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent selectionEvent) { pickAvatar(revamped); }
                    public void widgetSelected(SelectionEvent selectionEvent) { pickAvatar(revamped); }
                });
                _avatarImgStandard.add(img);
                pickAvatar(img);
            }
        }
    }
    
    private static final String T_AVATAR_OPEN_NAME = "syndie.gui.viewforum.avatar.name";
    private static final String T_AVATAR_OPEN_TYPE = "syndie.gui.viewforum.avatar.type";
    
    private void loadData() { loadData(_browser.getClient().getChannel(_scopeId)); }
    private void loadData(ChannelInfo info) {
        if (info != null) {
            _description.setText(str(info.getDescription()));
            
            if (info.getExpiration() > 0)
                _expiration.setText(Constants.getDate(info.getExpiration()));
            
            _name.setText(str(info.getName()));
            
            loadReferences(info);
            
            StringBuffer buf = new StringBuffer();
            for (Iterator iter = info.getPublicTags().iterator(); iter.hasNext(); )
                buf.append((String)iter.next()).append(" ");
            for (Iterator iter = info.getPrivateTags().iterator(); iter.hasNext(); )
                buf.append((String)iter.next()).append(" ");
            _tags.setText(buf.toString());
            
            if (_avatarImgOrig != null)
                pickAvatar(_avatarImgOrig);
            
            loadArchives(info);
            loadUsers(info);
            _root.layout(true, true);
        }
        _initialized = true;
        _modified = false;
        _save.setEnabled(false);
        _cancel.setEnabled(false);
    }
    private static final String str(String orig) { return (orig != null ? orig : ""); }
    private void loadReferences(ChannelInfo info) {
        _referenceNodeRoots = info.getReferences();
        redrawReferences();
    }
    private void redrawReferences() {
        _references.setRedraw(false);
        _references.removeAll();
        // place a depth first walk of the references into _references
        final List dfsNodes = new ArrayList();
        ReferenceNode.Visitor walker = new ReferenceNode.Visitor() {
            public void visit(ReferenceNode node, int depth, int siblingOrder) {
                dfsNodes.add(node);
                ReferenceNode parent = node.getParent();
                String parentString = null;
                if (parent != null)
                    parentString = _references.getItem(dfsNodes.indexOf(parent));
                String name = node.getName();
                String desc = node.getDescription();
                SyndieURI uri = node.getURI();
                StringBuffer buf = new StringBuffer();
                if (parentString != null) 
                    buf.append(parentString).append(" > ");
                if (name != null)
                    buf.append(name);
                else if (desc != null)
                    buf.append(desc);
                else if (uri != null)
                    buf.append(uri.toString());
                _references.add(buf.toString());
            }
        };
        ReferenceNode.walk(_referenceNodeRoots, walker);
        _referenceNodes = dfsNodes;
        if ( (!_editable) && (_referenceNodes.size() <= 0) ) {
            ((GridData)_references.getLayoutData()).exclude = true;
            ((GridData)_referencesLabel.getLayoutData()).exclude = true;
            //((GridData)_referencesAdd.getParent().getLayoutData()).exclude = true;
            //_referencesAdd.getParent().setVisible(false);
            _references.setVisible(false);
            _referencesLabel.setVisible(false);
        } else if (_editable && (_referenceNodes.size() <= 0)) {
            _references.add(_browser.getTranslationRegistry().getText(T_REFERENCES_ADD, "Add new references"));
        } else if (_editable) {
            _references.add(_browser.getTranslationRegistry().getText(T_REFERENCES_EDIT, "Manage references"));
        }
        _references.setRedraw(true);
    }
    private void loadArchives(ChannelInfo info) {
        // add buttons w/ menus for the archives in _archiveGroup
        _privArchiveURIs = new ArrayList();
        for (Iterator iter = info.getPrivateArchives().iterator(); iter.hasNext(); ) {
            ArchiveInfo archive = (ArchiveInfo)iter.next();
            _privArchiveURIs.add(archive.getURI());
        }
        _pubArchiveURIs = new ArrayList();
        for (Iterator iter = info.getPublicArchives().iterator(); iter.hasNext(); ) {
            ArchiveInfo archive = (ArchiveInfo)iter.next();
            _pubArchiveURIs.add(archive.getURI());
        }
        
        redrawArchives();
    }
    private void redrawArchives() {
        _archives.setRedraw(false);
        _archives.removeAll();
        _archiveItemToURI.clear();
        
        List all = new ArrayList();
        all.addAll(_pubArchiveURIs);
        all.addAll(_privArchiveURIs);
        for (int i = 0; i < all.size(); i++) {
            final SyndieURI uri = (SyndieURI)all.get(i);
            if (uri == null) continue;
            String url = uri.getURL();
            if ( (url == null) || (url.trim().length() <= 0) ) continue;
            TableItem item = new TableItem(_archives, SWT.NONE);
            item.setText(0, url.trim());
            if (_pubArchiveURIs.contains(uri))
                item.setText(1, _browser.getTranslationRegistry().getText(T_ARCHIVE_PUBLIC, "Public"));
            else
                item.setText(1, _browser.getTranslationRegistry().getText(T_ARCHIVE_PRIVATE, "Authorized readers only"));
            
            _archiveItemToURI.put(item, uri);
        }
        
        if ( (all.size() <= 0) && (!_editable) ) {
            _archiveGroup.setVisible(false);
            ((GridData)_archiveGroup.getLayoutData()).exclude = true;
        }
        
        _archiveURL.pack();
        _archiveIsPub.pack();
    
        _archives.setRedraw(true);
    }

    private static final String T_ARCHIVE_VIEW = "syndie.gui.viewforum.archive.view";
    private static final String T_ARCHIVE_DELETE = "syndie.gui.viewforum.archive.delete";
    private static final String T_ARCHIVE_ISPUB = "syndie.gui.viewforum.archive.ispub";
    private static final String T_ARCHIVE_ADD = "syndie.gui.viewforum.archive.add";
    private static final String T_ARCHIVE_PUBLIC = "syndie.gui.viewforum.archive.public";
    private static final String T_ARCHIVE_PRIVATE = "syndie.gui.viewforum.archive.private";
    
    private void addArchive() {
        LinkBuilderPopup popup = new LinkBuilderPopup(_browser, _parent.getShell(), new LinkBuilderPopup.LinkBuilderSource () {
                public void uriBuilt(SyndieURI uri, String text) {
                    if (uri != null) {
                        _privArchiveURIs.add(uri);
                        redrawArchives();
                        modified();
                    }
                }
                public int getPageCount() { return 0; }
                public List getAttachmentDescriptions() { return Collections.EMPTY_LIST; }
            });
        popup.limitOptions(false, false, false, false, false, false, false, false, false, true);
        popup.setShowText(false);
        popup.showPopup();
    }
    
    private void loadUsers(ChannelInfo info) {
        // add buttons w/ menus for the authorized managers and posters in _userGroup
        _managerHashes = new ArrayList(info.getAuthorizedManagerHashes());
        _posterHashes = new ArrayList(info.getAuthorizedPosterHashes());
        
        redrawUsers();
    }
    
    private void redrawUsers() {
        _users.setRedraw(false);
        _users.removeAll();
        _userItemToHash.clear();
        
        List all = new ArrayList();
        all.addAll(_managerHashes);
        all.addAll(_posterHashes);
        
        for (int i = 0; i < all.size(); i++) {
            final Hash scope = (Hash)all.get(i);
            String name = _browser.getClient().getChannelName(scope);
            
            TableItem item = new TableItem(_users, SWT.NONE);
            if (name != null)
                item.setText(0, scope.toBase64().substring(0,6) + ": " + name);
            else
                item.setText(0, scope.toBase64().substring(0,6));
            
            if (_managerHashes.contains(scope))
                item.setText(1, _browser.getTranslationRegistry().getText(T_USER_PRIV_MANAGE, "Manager"));
            else
                item.setText(1, _browser.getTranslationRegistry().getText(T_USER_PRIV_POST, "Authorized poster"));
            
            _userItemToHash.put(item, scope);
        }
        
        if ( (all.size() <= 0) && (!_editable) ) {
            _userGroup.setVisible(false);
            ((GridData)_userGroup.getLayoutData()).exclude = true;
        }
        
        _userName.pack();
        _userPriv.pack();
    
        _users.setRedraw(true);
    }
    private static final String T_USER_PRIV_MANAGE = "syndie.gui.viewforum.users.priv.manage";
    private static final String T_USER_PRIV_POST = "syndie.gui.viewforum.users.priv.post";
    private static final String T_USER_ADD = "syndie.gui.viewforum.user.add";
    private static final String T_USER_DELETE = "syndie.gui.viewforum.user.delete";
    private static final String T_USER_MANAGE = "syndie.gui.viewforum.user.manage";
    private static final String T_USER_VIEWFORUMMETA = "syndie.gui.viewforum.user.viewforummeta";
    private static final String T_USER_VIEWFORUM = "syndie.gui.viewforum.user.viewforum";
    private void toggleUserManagement(Hash scope) {
        if (_managerHashes.contains(scope)) {
            _managerHashes.remove(scope);
            if (!_posterHashes.contains(scope))
                _posterHashes.add(scope);
        } else {
            _posterHashes.remove(scope);
            if (!_managerHashes.contains(scope))
                _managerHashes.add(scope);
        }
        redrawUsers();
        _root.layout(true, true);
        modified();
    }
    private void addUser() {
        ReferenceChooserPopup popup = new ReferenceChooserPopup(_root.getShell(), _browser, new ReferenceChooserTree.AcceptanceListener() {
            public void referenceAccepted(SyndieURI uri) {
                Hash scope = uri.getScope();
                if (!_posterHashes.contains(scope) && !_managerHashes.contains(scope)) {
                    _posterHashes.add(scope);
                    redrawUsers();
                    modified();
                }
            }
            public void referenceChoiceAborted() {}
        }, T_USER_ADD_TITLE, "Select user to add");
        popup.show();
    }
    private static final String T_USER_ADD_TITLE = "syndie.gui.viewforum.users.add.title";
    private void deleteUser(Hash scope) {
        _managerHashes.remove(scope);
        _posterHashes.remove(scope);
        modified();
        redrawUsers();
        _root.layout(true, true);
    }
    
    public void applyTheme(Theme theme) {
        _nameLabel.setFont(theme.DEFAULT_FONT);
        _name.setFont(theme.DEFAULT_FONT);
        _tagsLabel.setFont(theme.DEFAULT_FONT);
        _tags.setFont(theme.DEFAULT_FONT);
        _descriptionLabel.setFont(theme.DEFAULT_FONT);
        _description.setFont(theme.DEFAULT_FONT);
        _authorizationLabel.setFont(theme.DEFAULT_FONT);
        _authorization.setFont(theme.DEFAULT_FONT);
        _expirationLabel.setFont(theme.DEFAULT_FONT);
        _expiration.setFont(theme.DEFAULT_FONT);
        _referencesLabel.setFont(theme.DEFAULT_FONT);
        _references.setFont(theme.DEFAULT_FONT);
        _userGroup.setFont(theme.DEFAULT_FONT);
        _archiveGroup.setFont(theme.DEFAULT_FONT);
        _save.setFont(theme.BUTTON_FONT);
        _cancel.setFont(theme.BUTTON_FONT);
        _keyManagementGroup.setFont(theme.DEFAULT_FONT);
        _keyManagementOpenInfo.setFont(theme.DEFAULT_FONT);
        _keyManagementRotateInfo.setFont(theme.DEFAULT_FONT);
        _keyManagementKeepInfo.setFont(theme.DEFAULT_FONT);
        _keyManagementResetInfo.setFont(theme.DEFAULT_FONT);
        _keyManagementPBEInfo.setFont(theme.DEFAULT_FONT);
        _keyManagementOpen.setFont(theme.DEFAULT_FONT);
        _keyManagementRotate.setFont(theme.DEFAULT_FONT);
        _keyManagementKeep.setFont(theme.DEFAULT_FONT);
        _keyManagementReset.setFont(theme.DEFAULT_FONT);
        _keyManagementPBE.setFont(theme.DEFAULT_FONT);
        _keyManagementNewReply.setFont(theme.DEFAULT_FONT);
        _users.setFont(theme.TABLE_FONT);
        _archives.setFont(theme.TABLE_FONT);
        
        if (_editable) {
            _avatarSelect.setFont(theme.BUTTON_FONT);
            _userAdd.setFont(theme.BUTTON_FONT);
            _archiveAdd.setFont(theme.BUTTON_FONT);
        }
        
        redrawArchives();
        redrawUsers();

        // this sizing stuff below is a mess.  problem being i'd like the 
        // keyManagementGroup's elements to be as wide as possible without
        // requiring a horizontal scrollbar, but the wrapping labels seem to
        // cause some trouble.  as it stands now, its just inconsistent, but
        // usually fits the text in there...
        
        //_root.layout(true, true);
        _root.pack(true);
        
        Rectangle sz = _keyManagementGroup.getClientArea();
        Point kgSz = _keyManagementGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point groupSz = _userGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point rootSz = _root.computeSize(groupSz.x, SWT.DEFAULT);
        Point parentSz = _parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point shellSz = _root.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point groupCurSz = _userGroup.getSize();
        Point rootCurSz = _root.getSize();
        Point parentCurSz = _parent.getSize();
        Point shellCurSz = _root.getShell().getSize();
        //_browser.getUI().debugMessage("computed group size: " + groupSz + " rootSz: " + rootSz + " parentSz: " + parentSz + " shellSz: " + shellSz);
        //_browser.getUI().debugMessage("current group size: " + groupCurSz + " rootSz: " + rootCurSz + " parentSz: " + parentCurSz + " shellSz: " + shellCurSz);
        //_browser.getUI().debugMessage("client area: " + sz + " kgSz: " + kgSz);
        
        int width = SWT.DEFAULT;
        if (parentCurSz.x <= 0) // first time
            width = parentSz.x;
        else
            width = sz.width-20; //SWT.DEFAULT; //_parent.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
        _browser.getUI().debugMessage("key management width: " + width);
        _keyManagementOpen.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementOpenInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementKeep.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementKeepInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementRotate.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementRotateInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementReset.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementResetInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementPBE.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementPBEInfo.setLayoutData(new RowData(width, SWT.DEFAULT));
        _keyManagementNewReply.setLayoutData(new RowData(width, SWT.DEFAULT));
        
        _scroll.setMinSize(rootSz);
        
        /*
        sz = _keyManagementGroup.getClientArea();
        groupSz = _userGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        rootSz = _root.computeSize(groupSz.x, SWT.DEFAULT);
        parentSz = _parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        shellSz = _root.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        groupCurSz = _userGroup.getSize();
        rootCurSz = _root.getSize();
        parentCurSz = _parent.getSize();
        shellCurSz = _root.getShell().getSize();
        _browser.getUI().debugMessage("after computed group size: " + groupSz + " rootSz: " + rootSz + " parentSz: " + parentSz + " shellSz: " + shellSz);
        _browser.getUI().debugMessage("after current group size: " + groupCurSz + " rootSz: " + rootCurSz + " parentSz: " + parentCurSz + " shellSz: " + shellCurSz);
        _browser.getUI().debugMessage("after client area: " + sz);
         */
    }

    private static final String T_NAME = "syndie.gui.viewforum.name";
    private static final String T_TAGS = "syndie.gui.viewforum.tags";
    private static final String T_DESC = "syndie.gui.viewforum.desc";
    private static final String T_AVATAR_SELECT = "syndie.gui.viewforum.avatar.select";
    private static final String T_AUTH = "syndie.gui.viewforum.auth";
    private static final String T_EXPIRATION = "syndie.gui.viewforum.expiration";
    private static final String T_REFERENCES = "syndie.gui.viewforum.references";
    private static final String T_REFERENCES_ADD = "syndie.gui.viewforum.references.add";
    private static final String T_REFERENCES_EDIT = "syndie.gui.viewforum.references.edit";
    private static final String T_USERS = "syndie.gui.viewforum.users";
    private static final String T_PUBARCHIVE = "syndie.gui.viewforum.pubarchive";
    private static final String T_PRIVARCHIVE = "syndie.gui.viewforum.privarchive";
    private static final String T_SAVE = "syndie.gui.viewforum.save";
    private static final String T_CANCEL = "syndie.gui.viewforum.cancel";
    private static final String T_KEYMGMT = "syndie.gui.viewforum.keymgmt";
    private static final String T_KEYMGMT_OPEN = "syndie.gui.viewforum.keymgmt.open";
    private static final String T_KEYMGMT_OPEN_INFO = "syndie.gui.viewforum.keymgmt.open.info";
    private static final String T_KEYMGMT_KEEP = "syndie.gui.viewforum.keymgmt.keep";
    private static final String T_KEYMGMT_KEEP_INFO = "syndie.gui.viewforum.keymgmt.keep.info";
    private static final String T_KEYMGMT_ROTATE = "syndie.gui.viewforum.keymgmt.rotate";
    private static final String T_KEYMGMT_ROTATE_INFO = "syndie.gui.viewforum.keymgmt.rotate.info";
    private static final String T_KEYMGMT_RESET = "syndie.gui.viewforum.keymgmt.reset";
    private static final String T_KEYMGMT_RESET_INFO = "syndie.gui.viewforum.keymgmt.reset.info";
    private static final String T_KEYMGMT_PBE = "syndie.gui.viewforum.keymgmt.pbe";
    private static final String T_KEYMGMT_PBE_INFO = "syndie.gui.viewforum.keymgmt.pbe.info";
    private static final String T_KEYMGMT_NEWREPLY = "syndie.gui.viewforum.keymgmt.newreply";

    private static final String T_AUTH_PUBLIC = "syndie.gui.viewforum.auth.public";
    private static final String T_AUTH_PUBREPLY = "syndie.gui.viewforum.auth.pubreply";
    private static final String T_AUTH_AUTH = "syndie.gui.viewforum.auth.auth";
    
    private static final String T_AVATAR_OTHER = "syndie.gui.viewforum.avatar.other";

    public void translate(TranslationRegistry registry) {
        _nameLabel.setText(registry.getText(T_NAME, "Name:"));
        _tagsLabel.setText(registry.getText(T_TAGS, "Tags:"));
        _descriptionLabel.setText(registry.getText(T_DESC, "Description:"));
        if (_avatarSelect != null)
            _avatarSelect.setText(registry.getText(T_AVATAR_SELECT, "Select..."));
        _authorizationLabel.setText(registry.getText(T_AUTH, "Authorization:"));
        _expirationLabel.setText(registry.getText(T_EXPIRATION, "Expiration:"));
        _referencesLabel.setText(registry.getText(T_REFERENCES, "References:"));
        _userGroup.setText(registry.getText(T_USERS, "Authorized managers and posters:"));
        _archiveGroup.setText(registry.getText(T_PUBARCHIVE, "Advertized archives:"));
        if (_editable) {
            _save.setText(registry.getText(T_SAVE, "Save changes"));
            _cancel.setText(registry.getText(T_CANCEL, "Cancel changes"));
            _keyManagementGroup.setText(registry.getText(T_KEYMGMT, "Advanced: forum key management:"));
            _keyManagementOpen.setText(registry.getText(T_KEYMGMT_OPEN, "Open access"));
            _keyManagementOpenInfo.setText(registry.getText(T_KEYMGMT_OPEN_INFO, "This creates a new key for reading messages and publicizes it so that anyone can read with it"));
            _keyManagementKeep.setText(registry.getText(T_KEYMGMT_KEEP, "Keep existing keys"));
            _keyManagementKeepInfo.setText(registry.getText(T_KEYMGMT_KEEP_INFO, "This uses existing keys, publicizing them if they used to be public.  If the keys were passphrase protected before however, new users will not be able to enter the passphrase and gain access to these existing keys.  To do so, require a passphrase again here as well."));
            _keyManagementRotate.setText(registry.getText(T_KEYMGMT_ROTATE, "Rotate keys for authorized readers"));
            _keyManagementRotateInfo.setText(registry.getText(T_KEYMGMT_ROTATE_INFO, "This creates a new key, encrypted so that only already authorized readers can access the new key"));
            _keyManagementReset.setText(registry.getText(T_KEYMGMT_RESET, "Reset all keys"));
            _keyManagementResetInfo.setText(registry.getText(T_KEYMGMT_RESET_INFO, "This creates a new key encrypted with another new key.  The key used can either be distributed to authorized readers manually (with links in private messages, keyfiles, etc) or shared in a passphrase encrypted post."));
            _keyManagementPBE.setText(registry.getText(T_KEYMGMT_PBE, "Require a passphrase to access the keys"));
            _keyManagementPBEInfo.setText(registry.getText(T_KEYMGMT_PBE_INFO, "If a passphrase is required, even already authorized users will need to know the passphrase, so consider posting that prior to a key rotation."));
            _keyManagementNewReply.setText(registry.getText(T_KEYMGMT_NEWREPLY, "Create a new forum reply key"));
            
            _archiveAdd.setText(_browser.getTranslationRegistry().getText(T_ARCHIVE_ADD, "Add"));
            _userAdd.setText(_browser.getTranslationRegistry().getText(T_USER_ADD, "Add"));
            
            _avatarOther.setText(_browser.getTranslationRegistry().getText(T_AVATAR_OTHER, "Other..."));
        }

        int auth = -1;
        if (_authorization.getItemCount() > 0)
            auth = _authorization.getSelectionIndex();
        else
            auth = 1;
        _authorization.setRedraw(false);
        _authorization.removeAll();
        // order correlates w/ AUTH_*
        _authorization.add(registry.getText(T_AUTH_PUBLIC, "Allow anyone to post to the forum"));
        _authorization.add(registry.getText(T_AUTH_PUBREPLY, "Allow anyone to reply to authorized posts"));
        _authorization.add(registry.getText(T_AUTH_AUTH, "Only allow authorized posters to post"));
        _authorization.select(auth);
        _authorization.setRedraw(true);
    }
    
    private static final int AUTH_PUBLIC = 0;
    private static final int AUTH_PUBREPLY = 1;
    private static final int AUTH_AUTH = 2;
}