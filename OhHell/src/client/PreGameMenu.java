package client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import common.Constants;
import common.GraphicsTools;
import common.OhcTextField;
import core.GameOptions;

public class PreGameMenu extends CanvasInteractable {
    public static final int menuWidth = 400;
    public static final int menuTeamGap = 10;
    public static final int teamMargin = 10;
    public static final int teamWidth = 200;
    
    private GameOptions options;
    
    private GameClient client;
    private GameCanvas canvas;
    private ClientPlayer myPlayer;
    
    private PreGameMenu menu = this;
    
    private class Team extends CanvasButton {
        public int y;
        public int teamNumber = -1;
        public List<ClientPlayer> members = new LinkedList<>();
        
        public Team() {
            super("");
        }
        
        @Override
        public int x() {
            return menu.x() + menuWidth + menuTeamGap + teamMargin;
        }

        @Override
        public int y() {
            return menu.y() + y;
        }

        @Override
        public int width() {
            return teamWidth - 2 * teamMargin;
        }

        @Override
        public int height() {
            return members.size() * 14 + 6;
        }
        
        @Override
        public boolean isEnabled() {
            return myPlayer != null && !myPlayer.isKibitzer();
        }
        
        @Override
        public void paint(Graphics graphics) {
            super.paint(graphics);
            graphics.setFont(GraphicsTools.font);
            graphics.setColor(Color.BLACK);
            int i = 0;
            for (ClientPlayer player : members) {
                GraphicsTools.drawStringJustified(graphics, 
                        player.getName(), x() + width() / 2, y() + height() / 2 + 14 * i - 14 * (members.size() - 1) / 2, 1, 1);
                i++;
            }
        }
        
        @Override
        public void click() {
            if (teamNumber != myPlayer.getTeam()) {
                client.reteam(teamNumber);
            }
        }
    }
    public ArrayList<Team> teams = new ArrayList<>();
    
    List<CanvasInteractable> interactables = new LinkedList<>();
    
    public PreGameMenu(GameClient client, GameCanvas canvas) {
        options = client.getGameOptions();
        this.client = client;
        this.canvas = canvas;
        
        teams.add(new Team() {
            @Override
            public int x() {
                return menu.x() + menuWidth + menuTeamGap + teamMargin;
            }

            @Override
            public int y() {
                return menu.y() + teamMargin;
            }

            @Override
            public int width() {
                return teamWidth - 2 * teamMargin;
            }

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
                client.reteam(-1);
            }
        });
        
        OhcTextField nameField = new OhcTextField("Name");
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    client.rename(nameField.getText());
                }
            }
        });
        interactables.add(new CanvasEmbeddedSwing(nameField, canvas) {
            private boolean showing = false;

            @Override
            public int x() {
                return menu.x() + 70;
            }

            @Override
            public int y() {
                return menu.y() + 30;
            }

            @Override
            public int width() {
                return 200;
            }

            @Override
            public int height() {
                return 30;
            }

            @Override
            public boolean isShown() {
                return menu.isShown();
            }
            
            @Override
            public void paint(Graphics graphics) {
                boolean menuIsShowing = isShown();
                if (!showing && menuIsShowing) {
                    nameField.setText(myPlayer != null ? myPlayer.getName() : client.getUsername());
                    showing = true;
                } else if (showing && !menuIsShowing) {
                    showing = false;
                }
                super.paint(graphics);
            }
        });

        interactables.add(new CanvasButton("Change name") {
            @Override
            public int x() {
                return menu.x() + 280;
            }

            @Override
            public int y() {
                return menu.y() + 30;
            }

            @Override
            public int width() {
                return 100;
            }

            @Override
            public int height() {
                return 30;
            }

            @Override
            public boolean isShown() {
                return menu.isShown();
            }

            @Override
            public void click() {
                client.rename(nameField.getText());
            }
        });

        interactables.add(new CanvasButton("") {
            @Override
            public int x() {
                return menu.x() + 220;
            }

            @Override
            public int y() {
                return menu.y() + 80;
            }

            @Override
            public int width() {
                return 20;
            }

            @Override
            public int height() {
                return 20;
            }

            @Override
            public boolean isShown() {
                return menu.isShown();
            }

            @Override
            public String text() {
                return myPlayer != null && myPlayer.isKibitzer() ? "x" : "";
            }

            @Override
            public void click() {
                client.toggleKibitzer();
            }
        });
        
        CanvasSpinner robotsSpinner = new CanvasSpinner(0) {
            @Override
            public int x() {
                return menu.x() + 220;
            }
            
            @Override
            public int y() {
                return menu.y() + 135;
            }
            
            @Override
            public int min() {
                return 0;
            }
            
            @Override
            public int max() {
                return Constants.maxPlayersWithRobots - (int) canvas.getPlayers().stream().filter(ClientPlayer::isHuman).count();
            }

            @Override
            public boolean isShown() {
                return menu.isShown();
            }

            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }
            
            @Override
            public void onUpdate() {
                options.setNumRobots(getValue());
                client.reportGameOptions(options);
            }
            
            @Override
            public void paint(Graphics graphics) {
                setValue(options.getNumRobots());
                super.paint(graphics);
            }
        };
        interactables.add(robotsSpinner);

        interactables.add(new CanvasButton("") {
            @Override
            public int x() {
                return menu.x() + 220;
            }

            @Override
            public int y() {
                return menu.y() + 180;
            }

            @Override
            public int width() {
                return 20;
            }

            @Override
            public int height() {
                return 20;
            }

            @Override
            public boolean isShown() {
                return menu.isShown();
            }

            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }

            @Override
            public String text() {
                return options.getD() == 2 ? "x" : "";
            }

            @Override
            public void click() {
                options.setD(3 - options.getD());
                client.reportGameOptions(options);
            }
        });
        
        interactables.add(new CanvasButton("") {
            @Override
            public int x() {
                return menu.x() + 220;
            }

            @Override
            public int y() {
                return menu.y() + 220;
            }

            @Override
            public int width() {
                return 20;
            }

            @Override
            public int height() {
                return 20;
            }

            @Override
            public boolean isShown() {
                return menu.isShown();
            }

            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }

            @Override
            public String text() {
                return options.isTeams() ? "x" : "";
            }

            @Override
            public void click() {
                options.setTeams(!options.isTeams());
                client.reportGameOptions(options);
            }
        });

        interactables.add(new CanvasButton("Start") {
            @Override
            public int x() {
                return menu.x() + 40;
            }

            @Override
            public int y() {
                return menu.y() + 280;
            }

            @Override
            public int width() {
                return 150;
            }

            @Override
            public int height() {
                return 40;
            }

            @Override
            public boolean isShown() {
                return menu.isShown();
            }

            @Override
            public boolean isEnabled() {
                return myPlayer != null && myPlayer.isHost();
            }

            @Override
            public void click() {
                client.readyPressed();
            }
        });

        interactables.add(new CanvasButton("Leave table") {
            @Override
            public int x() {
                return menu.x() + 210;
            }

            @Override
            public int y() {
                return menu.y() + 280;
            }

            @Override
            public int width() {
                return 150;
            }

            @Override
            public int height() {
                return 40;
            }

            @Override
            public boolean isShown() {
                return menu.isShown();
            }

            @Override
            public boolean isEnabled() {
                return myPlayer != null;
            }

            @Override
            public void click() {
                client.leaveGame();
            }
        });
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (isShown()) {
            myPlayer = canvas.getMyPlayer();
            
            graphics.setColor(new Color(255, 255, 255, 180));
            GraphicsTools.drawBox(graphics, x(), y(), menuWidth, height(), 20);
            graphics.setColor(Color.BLACK);
            graphics.setFont(GraphicsTools.fontBold);
            GraphicsTools.drawStringJustified(graphics, 
                    "Name:", x() + 60, y() + 45, 2, 1);
            GraphicsTools.drawStringJustified(graphics, 
                    "Join as kibitzer:", x() + 180, y() + 90, 2, 1);
            
            graphics.drawLine(x() + 10, y() + 115, x() + menuWidth - 10, y() + 115);
            
            GraphicsTools.drawStringJustified(graphics, 
                    "Robots:", x() + 180, y() + 150, 2, 1);
            GraphicsTools.drawStringJustified(graphics, 
                    "Double deck:", x() + 180, y() + 190, 2, 1);
            GraphicsTools.drawStringJustified(graphics, 
                    "Teams:", x() + 180, y() + 230, 2, 1);
            
            graphics.drawLine(x() + 10, y() + 265, x() + menuWidth - 10, y() + 265);
            graphics.setFont(GraphicsTools.font);
            
            if (options.isTeams()) {
                graphics.setColor(new Color(255, 255, 255, 180));
                GraphicsTools.drawBox(graphics, x() + menuWidth + menuTeamGap, y(), teamWidth, height(), 20);
                graphics.setColor(Color.BLACK);
                
                HashMap<Integer, List<ClientPlayer>> teamMap = new HashMap<>();
                for (ClientPlayer player : client.getPlayers()) {
                    if (!teamMap.containsKey(player.getTeam())) {
                        teamMap.put(player.getTeam(), new LinkedList<>(Arrays.asList(player)));
                    } else {
                        teamMap.get(player.getTeam()).add(player);
                    }
                }
                
                int y = teamMargin + 25;
                int teamIndex = 1;
                for (Integer teamNumber : teamMap.keySet()) {
                    Team team;
                    if (teamIndex >= teams.size()) {
                        team = new Team();
                        teams.add(team);
                    } else {
                        team = teams.get(teamIndex);
                    }
                    team.y = y;
                    team.teamNumber = teamNumber;
                    team.members = teamMap.get(teamNumber);
                    
                    y += team.height() + 5;
                    teamIndex++;
                }
                
                while (teamIndex < teams.size()) {
                    teams.remove(teamIndex);
                }
                
                for (Team team : teams) {
                    team.paint(graphics);
                }
            }
        }
        
        for (CanvasInteractable inter : interactables) {
            inter.paint(graphics);
        }
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            for (CanvasInteractable inter : interactables) {
                CanvasInteractable inter1 = inter.updateMoused(x, y);
                if (inter1 != null) {
                    ans = inter1;
                }
            }
            
            if (options.isTeams()) {
                for (Team team : teams) {
                    CanvasInteractable inter2 = team.updateMoused(x, y);
                    if (inter2 != null) {
                        ans = inter2;
                    }
                }
            }
        }
        return ans;
    }
}
