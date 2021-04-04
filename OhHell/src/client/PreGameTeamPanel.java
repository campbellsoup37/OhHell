package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import common.GraphicsTools;
import core.GameOptions;

public class PreGameTeamPanel extends CanvasInteractable {
    public static final int teamMargin = 10;
    public static final int teamWidth = 200;
    public static final int teamNameHeight = 15;
    public static final int memberHeight = 15;
    public static final int memberMargin = 2;
    
    private GameOptions options;
    
    private GameClient client;
    private GameCanvas canvas;
    private ClientPlayer myPlayer;
    
    private int h = 340;
    
    private ClientPlayer botSelected = null;
    
    private class TeamButton extends CanvasButton {
        public int y;
        public ClientTeam team;
        
        public class MemberButton extends CanvasButton {
            public int y;
            public ClientPlayer player;
            
            public MemberButton(ClientPlayer player) {
                super(player.getName());
                this.player = player;
            }
            
            @Override
            public int x() {
                return TeamButton.this.x() + 20;
            }
            
            @Override
            public int y() {
                return TeamButton.this.y() + y;
            }
            
            @Override
            public int width() {
                return TeamButton.this.width() - 40;
            }
            
            @Override
            public int height() {
                return memberHeight;
            }
            
            @Override
            public boolean isSelected() {
                return botSelected == player;
            }
            
            @Override
            public void click() {
                if (botSelected == player) {
                    botSelected = null;
                } else {
                    botSelected = player;
                }
            }
        }
        private HashMap<ClientPlayer, MemberButton> members = new HashMap<>();
        
        public TeamButton() {
            super("");
        }
        
        public TeamButton(ClientTeam team) {
            this();
            this.team = team;
            setThickBorderColor(GraphicsTools.colors[team.getIndex()]);
        }
        
        @Override
        public int x() {
            return PreGameTeamPanel.this.x() + teamMargin;
        }

        @Override
        public int y() {
            return PreGameTeamPanel.this.y() + y;
        }

        @Override
        public int width() {
            return teamWidth - 2 * teamMargin;
        }

        @Override
        public int height() {
            return team.getMembers().size() * (memberHeight + memberMargin) 
                    + teamNameHeight
                    - memberMargin + 10;
        }
        
        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            
            if (team != null) {
                graphics.setFont(GraphicsTools.fontBold);
                graphics.setColor(GraphicsTools.colors[team.getIndex()]);
                GraphicsTools.drawStringJustified(graphics, 
                        GraphicsTools.fitString(graphics, team.getName(), width() - 6), 
                        x() + width() / 2, y() + 3 + teamNameHeight / 2, 1, 1);
                graphics.setFont(GraphicsTools.font);
                graphics.setColor(Color.BLACK);
                
                int y1 = 5 + teamNameHeight;
                for (ClientPlayer player : team.getMembers()) {
                    if (!members.containsKey(player)) {
                        MemberButton newMember = new MemberButton(player);
                        members.put(player, newMember);
                    }
                    MemberButton memberButton = members.get(player);
                    memberButton.y = y1;
                    memberButton.paint(graphics);
                    y1 += memberButton.height() + memberMargin;
                }
            }
        }
        
        @Override
        public void click() {
            if (botSelected != null || !myPlayer.isKibitzer()) {
                ClientPlayer toMove = botSelected == null ? myPlayer : botSelected;
                if (team.getIndex() != toMove.getTeam()) {
                    client.reteam(toMove, team.getIndex());
                }
                botSelected = null;
            }
        }
        
        @Override
        public CanvasInteractable updateMoused(int x, int y) {
            CanvasInteractable ans = super.updateMoused(x, y);
            if (isMoused() && myPlayer != null && myPlayer.isHost()) {
                for (MemberButton member : members.values()) {
                    if (!member.player.isHuman()) {
                        CanvasInteractable member1 = member.updateMoused(x, y);
                        if (member1 != null) {
                            ans = member1;
                        }
                    }
                }
            }
            return ans;
        }
    }
    private HashMap<ClientTeam, TeamButton> teams = new HashMap<>();
    
    public ArrayList<TeamButton> teamButtons = new ArrayList<>();
    
    public PreGameTeamPanel(GameClient client, GameCanvas canvas) {
        options = client.getGameOptions();
        this.client = client;
        this.canvas = canvas;
        
        // New team button
        teamButtons.add(new TeamButton() {
            @Override
            public int height() {
                return 20;
            }
            
            @Override
            public String text() {
                return "Make new team";
            }
            
            @Override
            public void click() {
                ClientPlayer toMove = botSelected == null ? myPlayer : botSelected;
                client.reteam(toMove, -1);
                botSelected = null;
            }
        });
        
        // Randomize teams button
        teamButtons.add(new TeamButton() {
            @Override
            public int height() {
                return 20;
            }
            
            @Override
            public String text() {
                return "Randomize teams";
            }
            
            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }
            
            @Override
            public void click() {
                client.scrambleTeams();
            }
        });
    }
    
    @Override
    public int height() {
        return h;
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (isShown()) {
            myPlayer = canvas.getMyPlayer();
            
            graphics.setColor(new Color(255, 255, 255, 180));
            GraphicsTools.drawBox(graphics, x(), y(), width(), height(), 20);
            graphics.setColor(Color.BLACK);

            Set<ClientTeam> staleTeamButtons = new HashSet<>();
            staleTeamButtons.addAll(teams.keySet());
            
            int y = teamMargin;
            for (ClientTeam team : client.getTeams().values()) {
                if (!teams.containsKey(team)) {
                    TeamButton newTeam = new TeamButton(team);
                    teams.put(team, newTeam);
                }
                TeamButton teamButton = teams.get(team);
                if (team.getMembers() != null && !team.getMembers().isEmpty()) {
                    teamButton.y = y;
                    teamButton.paint(graphics);
                    y += teamButton.height() + 5;
                    staleTeamButtons.remove(team);
                }
            }
            
            for (ClientTeam team : staleTeamButtons) {
                teams.remove(team);
            }
            
            for (TeamButton teamButton : teamButtons) {
                teamButton.y = y;
                teamButton.paint(graphics);
                y += teamButton.height() + 5;
            }
            
            h = y - 5 + teamMargin;
        }
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            if (options.isTeams()) {
                for (TeamButton team : teams.values()) {
                    CanvasInteractable inter1 = team.updateMoused(x, y);
                    if (inter1 != null) {
                        ans = inter1;
                    }
                }
                
                for (TeamButton teamButton : teamButtons) {
                    CanvasInteractable inter1 = teamButton.updateMoused(x, y);
                    if (inter1 != null) {
                        ans = inter1;
                    }
                }
            }
        }
        return ans;
    }
}
