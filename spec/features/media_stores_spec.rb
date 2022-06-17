require "features/shared/authentication_error"
require "features/shared/authorization_error"

describe "Media Stores", type: :feature do
  let!(:database_store) { create(:media_store, :database) }
  let!(:filesystem_store) { create(:media_store, :filesystem) }
  let(:path) { "/media-service/stores/" }

  context "for public" do
    before do
      visit path
    end

    it "doesn't display user's dropdown in navbar" do
      expect(page).not_to have_css('.navbar a.dropdown-toggle')
    end

    it_displays "authentication error"
  end

  context "for an ordinary user" do
    let(:user) { create(:user) }

    before { sign_in }

    it_displays "authorization error"
  end

  context "for an user with admin role" do
    let(:user) { create(:user, :with_admin_role) }

    before { sign_in }

    it_displays "authorization error"
  end

  context "for an user with system admin role" do
    let(:user) { create(:user, :with_system_admin_role) }

    before do
      sign_in

      visit path
    end

    it "is navigable" do
      within first(".navbar") do
        find(".dropdown-toggle").click
        click_link "Media-Stores"
      end

      expect(current_path).to eq("/media-service/stores/")
      expect(page).to have_css("h2", text: "Media-Stores")

      within '#stores-page' do
        expect(page).to have_css('table')
        within 'table' do
          expect(page).to have_css('tr', text: 'legacy-file-store filesystem')
          expect(page).to have_css('tr', text: 'database database')
        end
      end
    end

    specify "page has links to users" do
      expect(page).to have_link("0", href: "/media-service/stores/legacy-file-store/users/")
      expect(page).to have_link("0", href: "/media-service/stores/database/users/")
    end

    specify "page has links to groups" do
      expect(page).to have_link("0", href: "/media-service/stores/legacy-file-store/groups/")
      expect(page).to have_link("0", href: "/media-service/stores/database/groups/")
    end
  end
end
