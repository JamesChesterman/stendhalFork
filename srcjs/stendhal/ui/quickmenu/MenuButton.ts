/***************************************************************************
 *                       Copyright © 2024 - Stendhal                       *
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Affero General Public License as        *
 *   published by the Free Software Foundation; either version 3 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 ***************************************************************************/

import { ButtonBase } from "./ButtonBase";
import { ui } from "../UI";

declare var stendhal: any;


/**
 * Button to open the main application menu.
 */
export class MenuButton extends ButtonBase {

	constructor() {
		super("menu");
	}

	protected override onClick(evt: Event) {
		ui.showApplicationMenu();
	}
}
