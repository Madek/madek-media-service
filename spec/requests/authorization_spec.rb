describe "Authorization" do
  context "with token" do
    let!(:settings) { create(:media_service_setting) }
    let(:request) { faraday_client_with_token.get("settings/") }
    let(:response) { request }
    let(:user) { create(:user) }
    let(:api_token) { create(:api_token, user: user) }
    let(:user_token) { api_token.token_hash }

    describe "GET request" do
      context "with invalid token" do
        let(:user_token) { "invalid" }

        it "responds with 401" do
          expect(response.status).to eq(401)
        end

        it "responds with error message" do
          expect(response.body).to eq("No token for this token-secret found!")
        end
      end

      context "when token is revoked" do
        let(:api_token) { create(:api_token, user: user, revoked: true) }

        it "responds with 401" do
          expect(response.status).to eq(401)
        end

        it "responds with error message" do
          expect(response.body).to eq("No token for this token-secret found!")
        end
      end

      context "when token's scope_read is set to false" do
        let(:api_token) { create(:api_token, user: user, scope_read: false) }

        it "responds with 403" do
          expect(response.status).to eq(403)
        end

        it "responds with error message" do
          expect(response.body)
            .to eq("The token is not allowed to read i.e. to use safe http verbs.")
        end
      end

      context "for an ordinary user" do
        it "responds with 500" do
          expect(response.status).to eq(500)
        end

        it "responds with error message" do
          expect(response.body).to eq("System-admin scope required")
        end
      end

      context "for an user with admin role" do
        let(:user) { create(:user, :with_admin_role) }

        it "responds with 500" do
          expect(response.status).to eq(500)
        end

        it "responds with error message" do
          expect(response.body).to eq("System-admin scope required")
        end
      end
        
      context "for an user with system admin role" do
        let(:user) { create(:user, :with_system_admin_role) }

        it "responds with success" do
          expect(response.status).to eq(200)
        end
      end
    end

    describe "unsafe HTTP methods" do
      %w(
        delete
        patch
        post
        put
      ).each do |http_method|
        describe "#{http_method.upcase} request" do
          let(:request) { faraday_client_with_token.public_send(http_method, "settings/") }
          let(:user) { create(:user, :with_system_admin_role) }
          let(:api_token) { create(:api_token, user: user, scope_write: false) }

          context "when token's scope_write is set to false" do
            it "responds with 403" do
              expect(response.status).to eq(403)
            end

            it "responds with error message" do
              expect(response.body)
                .to eq("The token is not allowed to write i.e. to use unsafe http verbs.")
            end
          end
        end
      end
    end
  end
end
