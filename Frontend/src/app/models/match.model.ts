export interface Match {
  match_id: number;
  player_character_id: number;
  opponent_character_id: number;
  player_final_score: number;
  opponent_final_score: number;
  winner: string;
  player_dice1?: number;
  player_dice2?: number;
  player_dice3?: number;
  opponent_dice1?: number;
  opponent_dice2?: number;
  opponent_dice3?: number;
}

